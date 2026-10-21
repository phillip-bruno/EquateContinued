package com.wolfcola.equatecontinued.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.google.android.material.color.MaterialColors;
import com.wolfcola.equatecontinued.Calculator;
import com.wolfcola.equatecontinued.R;
import com.wolfcola.equatecontinued.databinding.ActivityCalcBinding;

/**
 * Handles all calculator button setup: click, long-click, extra-long-click,
 * and the backspace hold-repeat logic. Extracted from CalcActivity to reduce
 * its size and isolate button wiring concerns.
 */
public class ButtonManager {

    public interface Callback {
        void onButtonPressed(String key);
        void onResetRequested();
        void onDrawerRequested();
        void onUnitViewToggle();
        void onRefreshDynamicUnits();
        void onPercentButtonSwapped(AnimatedHoldButton button, String newMain, String newSec);
    }

    /**
     * Wire up all calculator buttons.
     */
    public static void setup(ActivityCalcBinding b, Calculator calc, Callback callback) {
        // digits first, so a button's index is its digit value
        Button[] buttons = {
                b.zeroButton, b.oneButton, b.twoButton, b.threeButton, b.fourButton,
                b.fiveButton, b.sixButton, b.sevenButton, b.eightButton, b.nineButton,

                b.plusButton, b.minusButton, b.multiplyButton, b.divideButton, b.percentButton,
                b.decimalButton, b.equalsButton, b.clearButton,
                b.openParaButton, b.closeParaButton,
        };

        b.percentButton.setPrimaryText(calc.mPreferences.getPercentButMain());
        b.percentButton.setSecondaryText(calc.mPreferences.getPercentButSec());

        for (int i = 0; i < buttons.length; i++) {
            final Button button = buttons[i];
            final String digit = i < 10 ? String.valueOf(i) : null;

            button.setOnClickListener(view -> callback.onButtonPressed(
                    digit != null ? digit : resolveClickValue(view.getId(), calc)));

            button.setOnLongClickListener(view -> {
                int buttonId = view.getId();
                String buttonValue = "";
                if (buttonId == R.id.multiply_button) {
                    buttonValue = "^";
                } else if (buttonId == R.id.clear_button) {
                    callback.onResetRequested();
                } else if (buttonId == R.id.equals_button) {
                    callback.onDrawerRequested();
                } else if (buttonId == R.id.percent_button) {
                    if (calc.mPreferences.getPercentButSec().equals("EE")) {
                        buttonValue = "E";
                    } else {
                        buttonValue = "%";
                    }
                } else if (buttonId == R.id.nine_button) {
                    callback.onRefreshDynamicUnits();
                } else if (buttonId == R.id.minus_button) {
                    buttonValue = "n";
                } else if (buttonId == R.id.divide_button) {
                    buttonValue = "i";
                } else if (buttonId == R.id.eight_button) {
                    callback.onUnitViewToggle();
                } else if (buttonId == R.id.open_para_button) {
                    buttonValue = "[";
                } else if (buttonId == R.id.close_para_button) {
                    buttonValue = "]";
                } else {
                    return false;
                }

                if (!buttonValue.equals(""))
                    callback.onButtonPressed(buttonValue);
                return true;
            });

            if (button instanceof AnimatedHoldButton ahb) {
                ahb.setOnExtraLongClickListener(view -> {
                    int buttonId = view.getId();
                    if (buttonId == R.id.percent_button) {
                        String main = calc.mPreferences.getPercentButMain();
                        String sec = calc.mPreferences.getPercentButSec();
                        calc.mPreferences.setPercentButMain(sec);
                        calc.mPreferences.setPercentButSec(main);
                        callback.onPercentButtonSwapped(ahb, sec, main);
                    }
                });
            }
        }

        setupBackspaceHold(b, callback);
    }

    private static String resolveClickValue(int buttonId, Calculator calc) {
        if (buttonId == R.id.plus_button) return "+";
        if (buttonId == R.id.minus_button) return "-";
        if (buttonId == R.id.multiply_button) return "*";
        if (buttonId == R.id.divide_button) return "/";
        if (buttonId == R.id.percent_button) {
            return calc.mPreferences.getPercentButMain().equals("%") ? "%" : "E";
        }
        if (buttonId == R.id.decimal_button) return ".";
        if (buttonId == R.id.equals_button) return "=";
        if (buttonId == R.id.clear_button) return "c";
        if (buttonId == R.id.open_para_button) return "(";
        if (buttonId == R.id.close_para_button) return ")";
        if (buttonId == R.id.backspace_button) return "b";
        return "";
    }

    private static void setupBackspaceHold(ActivityCalcBinding b, Callback callback) {
        final Context context = b.getRoot().getContext();
        final Drawable normalDrawable = b.backspaceButton.getBackground();
        b.backspaceButton.setOnTouchListener(new View.OnTouchListener() {
            private static final int NUM_COLOR_CHANGES = 10;
            private final int BACKSPACE_REPEAT = ViewUtils.getLongClickTimeout(context);
            private final int COLOR_CHANGE_PERIOD = BACKSPACE_REPEAT / NUM_COLOR_CHANGES;
            private Handler mColorHoldHandler;
            private View mView;
            private int mInc;

            final Runnable mBackspaceColor = new Runnable() {
                private final int mStartColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, 0);
                private final int mEndColor = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorError, 0);

                @Override
                public void run() {
                    if (mInc == NUM_COLOR_CHANGES) {
                        callback.onButtonPressed("b");
                        mColorHoldHandler.postDelayed(this, 100);
                        return;
                    }
                    mColorHoldHandler.postDelayed(this, COLOR_CHANGE_PERIOD);

                    float deltaRed = (float) Color.red(mStartColor) + ((float) Color.red(mEndColor) - (float) Color.red(mStartColor)) * ((float) mInc * (float) mInc * (float) mInc) / ((float) NUM_COLOR_CHANGES * (float) NUM_COLOR_CHANGES * (float) NUM_COLOR_CHANGES);
                    int deltaGreen = Color.green(mStartColor) + ((Color.green(mEndColor) - Color.green(mStartColor)) * mInc) / NUM_COLOR_CHANGES;
                    int deltaBlue = Color.blue(mStartColor) + ((Color.blue(mEndColor) - Color.blue(mStartColor)) * mInc) / NUM_COLOR_CHANGES;

                    mView.setBackgroundColor(Color.argb(255, (int) deltaRed, deltaGreen, deltaBlue));
                    mInc++;
                }
            };

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        callback.onButtonPressed("b");

                        mView = view;
                        mInc = 0;

                        if (mColorHoldHandler != null) return true;
                        mColorHoldHandler = new Handler(Looper.getMainLooper());
                        mColorHoldHandler.postDelayed(mBackspaceColor, COLOR_CHANGE_PERIOD);

                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (mColorHoldHandler == null) return true;
                        view.setBackground(normalDrawable);

                        mColorHoldHandler.removeCallbacks(mBackspaceColor);
                        mColorHoldHandler = null;
                        break;
                }
                return false;
            }
        });
    }
}
