package com.wolfcola.equatecontinued.view;

import android.content.res.Resources;

import com.wolfcola.equatecontinued.Calculator;
import com.wolfcola.equatecontinued.ExpSeparatorHandler;
import com.wolfcola.equatecontinued.Expression;
import com.wolfcola.equatecontinued.R;
import com.wolfcola.equatecontinued.SISuffixHelper;

/**
 * Builds formatted display text from Calculator state. Handles separator
 * insertion, unit prefix/suffix, and HTML color formatting.
 */
public class DisplayFormatter {
    private final ExpSeparatorHandler mSepHandler = new ExpSeparatorHandler();
    private String mTextPrefix = "";
    private String mExpressionText = "";
    private String mTextSuffix = "";

    public String getPrefix() { return mTextPrefix; }
    public String getExpression() { return mExpressionText; }
    public ExpSeparatorHandler getSepHandler() { return mSepHandler; }

    /**
     * Computes formatted display state from the calculator.
     *
     * @return [selStart, selEnd] adjusted for separators and prefix
     */
    public int[] format(Calculator calc, Resources res) {
        mTextPrefix = "";
        mExpressionText = mSepHandler.getSepText(calc.toString());
        mTextSuffix = "";

        int selStart = mSepHandler.translateToSepIndex(calc.getSelectionStart());
        int selEnd = mSepHandler.translateToSepIndex(calc.getSelectionEnd());

        if (!calc.isExpressionInvalid() && calc.isUnitSelected()) {
            mTextSuffix = " " + calc.getCurrUnitType().getCurrUnit().toString();
            if (!calc.isSolved()) {
                mTextPrefix = res.getString(R.string.word_Convert) + " ";
                mTextSuffix += " " + res.getString(R.string.word_to) + ":";
                selStart += mTextPrefix.length();
                selEnd += mTextPrefix.length();
            }
        }

        if (calc.isSolved() &&
                calc.getNumberFormat() == Expression.NumFormat.ENGINEERING) {
            mTextSuffix = " " + SISuffixHelper.getSuffixName(mExpressionText);
        }

        return new int[]{selStart, selEnd};
    }

    /**
     * Returns the formatted display text with gray prefix/suffix.
     */
    public CharSequence toDisplayText() {
        return toDisplayText(mExpressionText);
    }

    /**
     * Returns the formatted display text with the given expression
     * (used during highlight animation).
     */
    public CharSequence toDisplayText(String expressionText) {
        return ViewUtils.fromHtml(
                "<font color='gray'>" + mTextPrefix + "</font>" +
                        expressionText +
                        "<font color='gray'>" + mTextSuffix + "</font>");
    }
}
