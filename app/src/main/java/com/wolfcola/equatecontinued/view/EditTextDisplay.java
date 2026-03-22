package com.wolfcola.equatecontinued.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.text.InputType;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.wolfcola.equatecontinued.Calculator;
import com.wolfcola.equatecontinued.R;

import java.util.ArrayList;
import java.util.Objects;

public class EditTextDisplay extends androidx.appcompat.widget.AppCompatEditText {
    private Calculator mCalc;
    private float mTextSize = 0f;
    private float mMinTextSize;
    private int mSelStart = 0;
    private int mSelEnd = 0;
    private final DisplayFormatter mFormatter = new DisplayFormatter();
    private ValueAnimator mColorAnimation;

    public EditTextDisplay(Context context) {
        this(context, null);
    }

    public EditTextDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUpEditText(context, attrs);
    }

    public EditTextDisplay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setUpEditText(context, attrs);
    }

    private void setUpEditText(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DynamicText, 0, 0);
        try {
            mMinTextSize = ta.getDimension(R.styleable.DynamicText_minimumTextSize,
                    getTextSize());
        } finally {
            ta.recycle();
        }
    }

    /**
     * Set the singleton calc to this EditText for its own use
     */
    public void setCalc(Calculator calc) {
        mCalc = calc;
    }

    /**
     * Disable soft keyboard from appearing, use in conjunction with
     * android:windowSoftInputMode="stateAlwaysHidden|adjustNothing"
     */
    public void disableSoftInputFromAppearing() {
        setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setTextIsSelectable(true);
    }

    /**
     * Updates the text and selection with current value from calc
     */
    public void updateTextFromCalc() {
        int[] sel = mFormatter.format(mCalc, getResources());
        mSelStart = sel[0];
        mSelEnd = sel[1];

        setText(mFormatter.toDisplayText());

        setupHighlighting();

        setSelection(mSelStart, mSelEnd);

        setCursorVisible(!mCalc.isSolved());
    }

    /**
     * Helper method to setup the highlighting
     */
    public void setupHighlighting() {
        if (mCalc.isHighlighted()) {
            int colorFrom = ContextCompat.getColor(getContext(), R.color.highlight_from);
            int colorTo = ContextCompat.getColor(getContext(), R.color.highlight_to);
            final int ANIMATE_DURR = 600; //ms
            mColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            mColorAnimation.addUpdateListener(animator -> {
                String expText;
                //if the highlight got canceled during the async animation update, cancel
                if (!mCalc.isHighlighted()) {
                    animator.cancel();
                    expText = mFormatter.getExpression();
                } else {
                    ArrayList<Integer> highList = mCalc.getHighlighted();
                    highList = mFormatter.getSepHandler().translateIndexListToSep(highList);
                    int color = (Integer) animator.getAnimatedValue();
                    String expression = mFormatter.getExpression();
                    int len = highList.size();
                    StringBuilder coloredExp = new StringBuilder(expression.substring(0, highList.get(0)));
                    for (int i = 0; i < len; i++) {
                        int finish = expression.length();
                        if (i != len - 1) finish = highList.get(i + 1);
                        coloredExp.append("<font color='").append(color).append("'>")
                                .append(expression.charAt(highList.get(i)))
                                .append("</font>")
                                .append(expression.substring(highList.get(i) + 1, finish));
                    }
                    expText = coloredExp.toString();
                }

                setText(mFormatter.toDisplayText(expText));

                setSelection(mSelStart, mSelEnd);
            });
            mColorAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    clearHighlighted();
                }
            });
            mColorAnimation.setDuration(ANIMATE_DURR);
            mColorAnimation.start();
        }
    }


    public void clearHighlighted() {
        mCalc.clearHighlighted();

        //only need to change the text if we have a animator running
        if (mColorAnimation != null && mColorAnimation.isRunning()) {
            setText(mFormatter.toDisplayText());

            setSelection(mSelStart, mSelEnd);
        }
    }

    /**
     * Sets the current selection to the end of the expression
     */
    public void setSelectionToEnd() {
        int expLen = mFormatter.getExpression().length() + mFormatter.getPrefix().length();
        setSelection(expLen, expLen);
    }


    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        layoutText();
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) layoutText();
    }

    /**
     * Helper method to size text
     */
    private void layoutText() {
        Paint paint = getPaint();
        if (mTextSize != 0f) paint.setTextSize(mTextSize);
        //if min text size is the same as normal size, just leave
        if (mMinTextSize == getTextSize()) return;
        float textWidth = paint.measureText(Objects.requireNonNull(getText()).toString());
        float boxWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float textSize = getTextSize();
        if (textWidth > boxWidth) {
            float scaled = textSize * boxWidth / textWidth;
            if (scaled < mMinTextSize)
                scaled = mMinTextSize;
            paint.setTextSize(scaled);
            mTextSize = textSize;
        }
    }


    /**
     * Custom paste and cut commands, leave the default copy operation
     */
    @Override
    public boolean onTextContextMenuItem(int id) {
        boolean consumed = true;

        switch (id) {
            case android.R.id.cut:
                ClipboardHelper.cut(getContext(),
                        Objects.requireNonNull(getText()).subSequence(
                                getSelectionStart(), getSelectionEnd()),
                        mCalc);
                break;
            case android.R.id.paste:
                ClipboardHelper.paste(getContext(), mCalc);
                break;
            case android.R.id.copy:
                consumed = super.onTextContextMenuItem(id);
        }
        //update the view with calc's selection and text
        updateTextFromCalc();
        return consumed;
    }


    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (mCalc != null) {
            int preLen = mFormatter.getPrefix().length();
            int expLen = mFormatter.getExpression().length();

            int fixedSelStart = mFormatter.getSepHandler().makeIndexValid(selStart - preLen) + preLen;
            int fixedSelEnd = mFormatter.getSepHandler().makeIndexValid((selEnd - preLen)) + preLen;

            if (fixedSelStart != selStart || fixedSelEnd != selEnd) {
                setSelection(fixedSelStart, fixedSelEnd);
                return;
            }

            //check to see if the unit part of the expression has been selected
            if (selEnd > expLen + preLen) {
                setSelection(selStart, expLen + preLen);
                return;
            }
            if (selStart > expLen + preLen) {
                setSelection(expLen + preLen, selEnd);
                return;
            }
            if (selEnd < preLen) {
                setSelection(selStart, preLen);
                return;
            }
            if (selStart < preLen) {
                setSelection(preLen, selEnd);
                return;
            }

            //save the new selection in the calc class
            mCalc.setSelection(mFormatter.getSepHandler().translateFromSepIndex(selStart - preLen),
                    mFormatter.getSepHandler().translateFromSepIndex(selEnd - preLen));
            setCursorVisible(true);
        }
    }

}
