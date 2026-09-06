package com.wolfcola.equatecontinued.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import com.wolfcola.equatecontinued.R;

/**
 * Acts as a regular text view, but will dyamicly scale the font size down to
 * a certain size before going to two lines.  Min size will be specified in the
 * XML.  Created by Evan on 12/10/2016.
 */
public class DynamicTextView extends androidx.appcompat.widget.AppCompatTextView {
    private float mOriginalTextSize = 0f;
    private float mMinTextSize;

    public DynamicTextView(Context context) {
        super(context);
    }

    public DynamicTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUpTextView(context, attrs);
    }

    public DynamicTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setUpTextView(context, attrs);
    }


    private void setUpTextView(Context context, AttributeSet attrs) {
        //grab custom resource variable
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DynamicText, 0, 0);
        try {
            mMinTextSize = ta.getDimension(R.styleable.DynamicText_minimumTextSize,
                    getTextSize());
        } finally {
            ta.recycle();
        }
        //Log.d("DYN", "mStartingTextSize = " + getTextSize());
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
     * Helper method to size text dynamically depending on the text size in
     * relation to the width of the text view
     */
    private void layoutText() {
        if (getText().equals("")) return;

        // Remember the size the view started with (from XML) so repeated
        // calls always scale down from the same baseline instead of
        // compounding a previous shrink.
        if (mOriginalTextSize == 0f)
            mOriginalTextSize = getTextSize();
        //if min text size is the same as normal size, just leave
        if (mMinTextSize == mOriginalTextSize) return;

        float boxWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        // if the view doesn't exist, the box width will be 0 or negative
        if (boxWidth <= 0f) return;

        // Measure with a throwaway copy of the paint at the original size so
        // we don't disturb the TextView's own paint/layout while probing.
        Paint measurePaint = new Paint(getPaint());
        measurePaint.setTextSize(mOriginalTextSize);
        float textWidth = measurePaint.measureText(getText().toString());

        float targetSize = mOriginalTextSize;
        if (textWidth > boxWidth) {
            targetSize = mOriginalTextSize * boxWidth / textWidth;
            if (targetSize < mMinTextSize)
                targetSize = mMinTextSize;
        }

        if (getTextSize() != targetSize) {
            // Use the real setTextSize() API (not paint.setTextSize()) so the
            // TextView rebuilds its internal layout/line-height for the new
            // size, instead of drawing smaller glyphs inside line spacing
            // that was measured for the original (larger) size - which is
            // what left the stale blank gap above the text.
            setTextSize(TypedValue.COMPLEX_UNIT_PX, targetSize);
        }
    }
}
