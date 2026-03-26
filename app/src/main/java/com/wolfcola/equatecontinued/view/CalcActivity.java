package com.wolfcola.equatecontinued.view;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.wolfcola.equatecontinued.Calculator;
import com.wolfcola.equatecontinued.R;
import com.wolfcola.equatecontinued.view.IdlingResource.SimpleIdlingResource;

import java.util.HashSet;
import java.util.Set;

public class CalcActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String PRIVATE_PREF = "equate_app";
    private static final String VERSION_KEY = "version_number";

    // Fixes Resources$NotFoundException on API < 19 when using vector drawables
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private ResultListFragment mResultListFrag;   //scroll-able history
    private EditTextDisplay mDisplay;      //main display
    private ViewPager2 mUnitTypeViewPager;         //controls and displays UnitType
    private DynamicTextView mResultPreview;   //Result preview
    private UnitSearchDialogBuilder mSearchDialogBuilder; // Unit search dialog

    // (Used for test) Idling Resource which will be null in production.
    @Nullable
    private SimpleIdlingResource mIdlingResource;

    private Button mEqualsButton; //used for changing color
    private CalcViewModel mViewModel;
    //main calculator object
    private Calculator mCalc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //initialize ViewModel (survives configuration changes)
        mViewModel = new ViewModelProvider(this).get(CalcViewModel.class);
        //either get old calc or create a new one
        mCalc = mViewModel.getCalc();

        // Observe ViewModel events from fragments
        mViewModel.getUpdateScreen().observe(this, updateResult -> {
            if (updateResult != null) updateScreen(updateResult);
        });
        mViewModel.getUnitSelected().observe(this, selected -> {
            if (selected != null) setEqualButtonColor(selected);
        });
        mViewModel.getSelectUnitEvent().observe(this, event -> {
            if (event != null) selectUnitAtUnitArrayPos(event.unitPos, event.unitTypeKey);
        });

        //main result display
        mDisplay = (EditTextDisplay) findViewById(R.id.textDisplay);
        mResultPreview = (DynamicTextView) findViewById(R.id.resultPreview);
        mDisplay.setCalc(mCalc);
        mDisplay.disableSoftInputFromAppearing();


        //we don't want the text view to go to two lines ever. this fixes that
        mResultPreview.setHorizontallyScrolling(true);

        mResultPreview.setOnClickListener(v -> numButtonPressed("="));

        mResultPreview.setOnLongClickListener(v -> {
            CharSequence copiedText = mResultPreview.getText();

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(null, copiedText));

            ViewUtils.toast("Copied: \"" + copiedText + "\"", CalcActivity.this);
            return true;
        });

        //keyboard hiding wasn't working on Samsung device, brute force instead
        mDisplay.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mDisplay.getWindowToken(), 0);
        });

        //hold click will select all text
        mDisplay.setOnLongClickListener(v -> {
            mDisplay.selectAll();
            return false;
        });

        //clicking display will set solve=false, and will make the cursor visible
        mDisplay.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //once the user clicks on part of the expression, don't want # to delete it
                mCalc.setSolved(false);
                mDisplay.setCursorVisible(true);
                mDisplay.clearHighlighted();
            }
            return false;
        });

        //use fragment manager to make the result list
        FragmentManager fm = getSupportFragmentManager();
        mResultListFrag = (ResultListFragment) fm.findFragmentById(R.id.resultListFragmentContainer);

        if (mResultListFrag == null) {
            mResultListFrag = new ResultListFragment();
            fm.beginTransaction().add(R.id.resultListFragmentContainer, mResultListFrag).commit();
        }


        mEqualsButton = ButtonManager.setup(this, mCalc, new ButtonManager.Callback() {
            @Override
            public void onButtonPressed(String key) {
                numButtonPressed(key);
            }

            @Override
            public void onResetRequested() {
                resetDialog();
            }

            @Override
            public void onDrawerRequested() {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                drawer.openDrawer(GravityCompat.START);
            }

            @Override
            public void onUnitViewToggle() {
                setUnitViewVisibility(UnitVisibility.TOGGLE);
            }

            @Override
            public void onRefreshDynamicUnits() {
                mCalc.refreshAllDynamicUnits(true);
            }

            @Override
            public void onPercentButtonSwapped(AnimatedHoldButton button, String newMain, String newSec) {
                ViewUtils.toastLong("Button changed to " + newMain, CalcActivity.this);
                button.setPrimaryText(newMain);
                button.setSecondaryText(newSec);
                button.invalidate();
            }
        });

        showWhatsNewDialog();
    }

    private void setupUnitTypePager() {
        //if we have no Unit Types selected from settings, don't show Units view
        if (mCalc.getUnitTypeSize() == 0) {
            setUnitViewVisibility(UnitVisibility.HIDDEN);
            return;
        } else {
            setUnitViewVisibility(UnitVisibility.VISIBLE);
        }

        mUnitTypeViewPager = (ViewPager2) findViewById(R.id.unit_pager);
        mUnitTypeViewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return mCalc.getUnitTypeSize();
            }

            @NonNull
            @Override
            public Fragment createFragment(int pos) {
                return ConvKeysFragment.newInstance(pos);
            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.unit_type_titles);
        tabLayout.setVisibility(View.VISIBLE);

        new TabLayoutMediator(tabLayout, mUnitTypeViewPager,
                (tab, pos) -> tab.setText(mCalc.getUnitTypeName(pos % mCalc.getUnitTypeSize()))
        ).attach();

        //need to tell calc when a new UnitType page is selected
        mUnitTypeViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int pos) {
                // clear unit selection from current Unit Type before switching
                mCalc.getCurrUnitType().clearUnitSelection();

                //update the calc with current UnitType selection
                mCalc.setCurrentUnitTypePos(pos);

                //if we just switched to a dynamic unit, attempt an update
                if (mCalc.getCurrUnitType().containsDynamicUnits())
                    mCalc.refreshAllDynamicUnits(false);

                //clear selected unit from adjacent convert key fragment so you
                //a bit of it
                int currUnitTypePos = mUnitTypeViewPager.getCurrentItem();
                clearUnitSelection(currUnitTypePos - 1);
                clearUnitSelection(currUnitTypePos);
                clearUnitSelection(currUnitTypePos + 1);
                mCalc.getCurrUnitType().clearUnitSelection();

                //if this change in UnitType was result of unit-ed result selection,
                // select that unit
                if (mViewModel.getUnitPosToSelectAfterScroll() != -1) {
                    ConvKeysFragment frag = getConvKeyFrag(mUnitTypeViewPager.getCurrentItem());
                    if (frag != null)
                        frag.selectUnitAtUnitArrayPos(mViewModel.getUnitPosToSelectAfterScroll());
                    mViewModel.clearUnitPosToSelectAfterScroll();
                }

                //clear out the unit in expression if it's now cleared
                updateScreen(true);

                //move the cursor to the right end (helps usability a bit)
                mDisplay.setSelectionToEnd();
            }
        });

        //set page back to the previously selected page
        mUnitTypeViewPager.setCurrentItem(mCalc.getUnitTypePos(), false);
    }

    /**
     * Called when any non convert key is pressed
     *
     * @param keyPressed ASCII representation of the key pressed ("1", "=" "*", etc)
     */
    public void numButtonPressed(String keyPressed) {
        //pass button value to CalcActivity to pass to calc
        Calculator.CalculatorResultFlags flags = mCalc.parseKeyPressed(keyPressed);

        if (flags.createDiffUnitDialog) {
            new AlertDialog.Builder(this)
                    .setMessage(getText(R.string.click_another_unit))
                    .setPositiveButton(android.R.string.ok, null) //null cancels dialog
                    .show();
        }

        //update the result list and do it with the normal scroll (not fast)
        updateScreen(flags.performedSolve);
    }

    /**
     * Helper function to setup the dialog used to reset the calculator.
     */
    private void resetDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getText(R.string.reset_title))
                .setItems(new CharSequence[]
                                {getText(R.string.reset_clear_history), getText(R.string.reset_factory)},
                        (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    clearHistory();
                                    break;
                                case 1:
                                    new AlertDialog.Builder(CalcActivity.this)
                                            .setMessage(getText(R.string.reset_factory_msg))
                                            .setPositiveButton(android.R.string.yes, (d, w) -> resetCalculator())
                                            .setNegativeButton(android.R.string.cancel, null)
                                            .show();
                                    break;
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    /**
     * Clears the history,  updates the screen, and toasts the user
     */
    public void clearHistory() {
        mCalc.clearResultList();

        updateScreen(true);

        ViewUtils.toastCentered("History cleared", this);
    }

    /**
     * Perform a full reset of the calculator.  Clears expression, history,
     * preferences, and resets the calculator to original state.
     */
    public void resetCalculator() {
        mCalc.resetCalc();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        setupUnitTypePager();

        updateScreen(true);

        ViewUtils.toastCentered("Calculator reset", this);
    }

    /**
     * Helper function to create a show what's new dialog when user first opens
     * this version of the app
     */
    private void showWhatsNewDialog() {
        SharedPreferences sharedPref = getSharedPreferences(PRIVATE_PREF, Context.MODE_PRIVATE);
        int currentVersionNumber = 0;

        int savedVersionNumber = sharedPref.getInt(VERSION_KEY, 0);

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersionNumber = pi.versionCode;
        } catch (Exception e) {
            android.util.Log.e("CalcActivity", "Failed to get version info", e);
        }

        if (currentVersionNumber > savedVersionNumber) {
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.dialog_whatsnew, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(getText(R.string.whats_new))
                    .setMessage(getText(R.string.version_description))
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

            builder.create().show();

            SharedPreferences.Editor editor = sharedPref.edit();

            editor.putInt(VERSION_KEY, currentVersionNumber);
            editor.apply();
        }
    }


    /**
     * Selects a unit (used by result list via ViewModel)
     */
    public void selectUnitAtUnitArrayPos(int unitPos, String unitTypeKey) {
        int visibleUnitTypeIndex = mCalc.getUnitTypeIndex(unitTypeKey);

        // if Unit Type is not displayed, update prefs to set it as displayed
        if (visibleUnitTypeIndex == -1) {
            //load in Unit Type arrangement prefs
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            Set<String> storedSet = sharedPref.getStringSet(
                    SettingsActivity.UNIT_TYPE_PREF_KEY, null);

            assert storedSet != null; // not sure why we'd have a null pref
            HashSet<String> selections = new HashSet<>(storedSet);
            selections.add(unitTypeKey);
            sharedPref.edit().putStringSet(
                    SettingsActivity.UNIT_TYPE_PREF_KEY, selections).apply();

            // update the selections in the calculator
            mCalc.setSelectedUnitTypes(selections);
            visibleUnitTypeIndex = mCalc.getUnitTypeIndex(unitTypeKey);

            // update our unit pager to reflect updated prefs
            setupUnitTypePager();
        }
        //if not on right page, scroll there first
        if (visibleUnitTypeIndex != mUnitTypeViewPager.getCurrentItem()) {
            mViewModel.setUnitPosToSelectAfterScroll(unitPos);
            mUnitTypeViewPager.setCurrentItem(visibleUnitTypeIndex);
        } else {
            ConvKeysFragment frag = getConvKeyFrag(mUnitTypeViewPager.getCurrentItem());
            if (frag != null) frag.selectUnitAtUnitArrayPos(unitPos);
        }
    }

    private void setUnitViewVisibility(UnitVisibility uv) {
        final LinearLayout mUnitContain = (LinearLayout) findViewById(R.id.unit_container);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (uv == UnitVisibility.HIDDEN || mCalc.getUnitTypeSize() == 0 ||
                    (uv == UnitVisibility.TOGGLE && mUnitContain.getVisibility() == LinearLayout.VISIBLE))
                mUnitContain.setVisibility(LinearLayout.GONE);
            else {
                mUnitContain.setVisibility(LinearLayout.VISIBLE);
                //update the screen to move result list up
                updateScreen(true, true);
            }
        }
    }

    /**
     * Grabs newest data from Calculator, updates the main display, and gives an
     * option to scroll down the result list
     *
     * @param updateResult  pass true to update result list
     * @param instantScroll pass true to scroll instantly, otherwise use animation
     */
    private void updateScreen(boolean updateResult, boolean instantScroll) {
        mDisplay.updateTextFromCalc(); //Update EditText view

        //will preview become visible during this screen update?
        boolean makePreviewVisible = !mCalc.isSolved()
                && !mCalc.isPreviewEmpty() && !mCalc.isUnitSelected();

        //if preview just appeared, move the history list up so the last item
        //doesn't get hidden by the preview
        if (mResultPreview.getVisibility() != View.VISIBLE && makePreviewVisible) {
            updateResult = true;
            instantScroll = true;
        }

        mResultPreview.setVisibility(makePreviewVisible ? View.VISIBLE : View.GONE);

        updatePreviewText(MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0));

        //if we hit equals, update result list
        if (updateResult)
            mResultListFrag.refresh(instantScroll);
    }

    /**
     * Grabs newest data from Calculator, updates the main display
     *
     * @param updateResult whether or not to update result
     */
    public void updateScreen(boolean updateResult) {
        //no instant scroll for previous expression
        updateScreen(updateResult, false);

        //see if colored convert button should be not colored (if backspace or
        //clear were pressed, or if expression solved)
        if (!mCalc.isUnitSelected() && mUnitTypeViewPager != null)
            clearUnitSelection(mUnitTypeViewPager.getCurrentItem());
    }

    private void updatePreviewText(int suffixColor) {
        mResultPreview.setText(mCalc.getPreviewText(suffixColor));
    }

    /**
     * Changes equals button color according the the input boolean value.
     * Equals button is colored normally when button is not selected. When
     * a unit is selected, equals button looks like a regular op button
     */
    public void setEqualButtonColor(boolean unHighlighted) {
        mEqualsButton.setSelected(unHighlighted);
    }

    /**
     * Clear the unit selection for unit type fragment at position pos
     *
     * @param unitTypeFragPos the position of the desired unit type fragment
     *                        from which to clear selected units
     */
    private void clearUnitSelection(int unitTypeFragPos) {
        ConvKeysFragment currFragAtPos = getConvKeyFrag(unitTypeFragPos);
        if (currFragAtPos != null)
            currFragAtPos.clearButtonSelection();
    }

    /**
     * Helper function to return the convert key fragment at position pos
     *
     * @param pos the position of the desired convert key fragment
     * @return will return the fragment or null if it doesn't exist at that position
     */
    private ConvKeysFragment getConvKeyFrag(int pos) {
        if (mUnitTypeViewPager.getAdapter() == null) return null;
        //make sure we aren't trying to access an invalid page fragment
        if (pos < mUnitTypeViewPager.getAdapter().getItemCount() && pos >= 0) {
            return (ConvKeysFragment) getSupportFragmentManager()
                    .findFragmentByTag("f" + pos);
        } else return null;
    }

    /**
     * Called when an item in the navigation menu drawer is selected
     *
     * @param item that is selected
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_find) {
            if (mSearchDialogBuilder == null) {
                mSearchDialogBuilder = new UnitSearchDialogBuilder(mCalc.getUnitTypeList());
            }

            mSearchDialogBuilder.buildDialog(this,
                    getString(R.string.find_unit),
                    mIdlingResource,
                    (parent, view, position, searchId) -> {
                        mSearchDialogBuilder.cancelDialog();
                        UnitSearchItem searchItem = mSearchDialogBuilder.getItem(position);
                        selectUnitAtUnitArrayPos(searchItem.getUnitPosition(), searchItem.getUnitTypeKey());
                    });
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_about) {
            PackageInfo pInfo;
            String version = "unknown";
            try {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                version = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            new AlertDialog.Builder(this)
                    .setTitle(getText(R.string.about_title))
                    .setMessage(getText(R.string.about_version) + version +
                            "\n\n" + getText(R.string.about_message))
                    .setPositiveButton(android.R.string.yes, null)
                    .show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            Calculator.getCalculator(this).saveState();
        } catch (Exception e) {
            android.util.Log.e("CalcActivity", "Failed to save calculator state", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //maybe fixes that random crash?
        if (mCalc == null)
            return;

        //load in Unit Type arrangement prefs
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> selections = sharedPref.getStringSet(
                SettingsActivity.UNIT_TYPE_PREF_KEY, null);

        // determine if user changed the configuration of the Unit Types
        mCalc.setSelectedUnitTypes(selections);

        setupUnitTypePager();

        // if the Unit Type configuration changed, update tab indicator accordingly
//		if (selectionChanged)

        if (mCalc.getCurrUnitType().containsDynamicUnits())
            mCalc.refreshAllDynamicUnits(false);

        //only set display to Equate if no expression is there yet
        if (mCalc.toString().equals("") && mCalc.getResultList().size() == 0) {
            mDisplay.setText(R.string.app_name);
            mDisplay.setCursorVisible(false);
        } else {
            //	updateScreen(true, true);
            mDisplay.setSelectionToEnd();
            //pull ListFrag's focus, to be sure EditText's cursor blinks when app starts
            mDisplay.requestFocus();
        }

    }

    /**
     * Only called from test, creates and returns a new {@link SimpleIdlingResource}.
     */
//	@VisibleForTesting
    @NonNull
    public SimpleIdlingResource getIdlingResource() {
        if (mIdlingResource == null) {
            mIdlingResource = new SimpleIdlingResource();
        }
        return mIdlingResource;
    }

    public enum UnitVisibility {VISIBLE, HIDDEN, TOGGLE}
}
