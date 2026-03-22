package com.wolfcola.equatecontinued.view;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;

import com.wolfcola.equatecontinued.Calculator;
import com.wolfcola.equatecontinued.R;

/**
 * Handles all calculator button setup: click, long-click, extra-long-click,
 * and the backspace hold-repeat logic. Extracted from CalcActivity to reduce
 * its size and isolate button wiring concerns.
 */
public class ButtonManager {

    private static final int[] BUTTON_IDS = {
            R.id.zero_button, R.id.one_button, R.id.two_button, R.id.three_button,
            R.id.four_button, R.id.five_button, R.id.six_button, R.id.seven_button,
            R.id.eight_button, R.id.nine_button,

            R.id.plus_button,
            R.id.minus_button,
            R.id.multiply_button,
            R.id.divide_button,
            R.id.percent_button,

            R.id.decimal_button,
            R.id.equals_button,

            R.id.clear_button,

            R.id.open_para_button,
            R.id.close_para_button,
    };

    public interface Callback {
        void onButtonPressed(String key);
        void onResetRequested();
        void onDrawerRequested();
        void onUnitViewToggle();
        void onRefreshDynamicUnits();
        void onPercentButtonSwapped(AnimatedHoldButton button, String newMain, String newSec);
    }

    /**
     * Wire up all calculator buttons. Returns the equals button reference.
     */
    public static Button setup(Activity activity, Calculator calc, Callback callback) {
        Button equalsButton = null;

        for (int id : BUTTON_IDS) {
            final Button button = (Button) activity.findViewById(id);

            if (id == R.id.equals_button) equalsButton = button;

            if (id == R.id.percent_button) {
                ((AnimatedHoldButton) button)
                        .setPrimaryText(calc.mPreferences.getPercentButMain());
                ((AnimatedHoldButton) button)
                        .setSecondaryText(calc.mPreferences.getPercentButSec());
            }

            button.setOnClickListener(view -> {
                String buttonValue = resolveClickValue(view.getId(), calc);
                callback.onButtonPressed(buttonValue);
            });

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

            if (button instanceof AnimatedHoldButton) {
                final AnimatedHoldButton ahb = (AnimatedHoldButton) button;
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

        setupBackspaceHold(activity, callback);

        return equalsButton;
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

        // Check numerical buttons
        for (int i = 0; i < 10; i++) {
            if (buttonId == BUTTON_IDS[i]) {
                return String.valueOf(i);
            }
        }
        return "";
    }

    private static void setupBackspaceHold(Activity activity, Callback callback) {
        ImageButton backspaceButton = (ImageButton) activity.findViewById(R.id.backspace_button);
        backspaceButton.setOnTouchListener(new View.OnTouchListener() {
            private static final int NUM_COLOR_CHANGES = 10;
            private final int BACKSPACE_REPEAT = ViewUtils.getLongClickTimeout(activity);
            private final int COLOR_CHANGE_PERIOD = BACKSPACE_REPEAT / NUM_COLOR_CHANGES;
            private Handler mColorHoldHandler;
            private View mView;
            private int mInc;

            Runnable mBackspaceColor = new Runnable() {
                private int mStartColor = ContextCompat.getColor(activity, R.color.op_button_pressed);
                private int mEndColor = ContextCompat.getColor(activity, R.color.backspace_button_held);

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
                        mColorHoldHandler = new Handler();
                        mColorHoldHandler.postDelayed(mBackspaceColor, COLOR_CHANGE_PERIOD);

                        break;
                    case MotionEvent.ACTION_UP:
                        if (mColorHoldHandler == null) return true;
                        view.setBackgroundColor(ContextCompat.getColor(activity, R.color.op_button_normal));

                        mColorHoldHandler.removeCallbacks(mBackspaceColor);
                        mColorHoldHandler = null;
                        break;
                }
                return false;
            }
        });
    }
}
