package com.wolfcola.equatecontinued.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.SystemClock;
import android.widget.Toast;

import com.wolfcola.equatecontinued.Calculator;
import com.wolfcola.equatecontinued.R;

/**
 * Handles cut and paste operations for the calculator display.
 */
public class ClipboardHelper {
    static long LAST_CUT_OR_COPY_TIME;

    public static void cut(Context context, CharSequence selectedText, Calculator calc) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, selectedText));

        calc.parseKeyPressed("b");

        LAST_CUT_OR_COPY_TIME = SystemClock.uptimeMillis();

        Toast.makeText(context,
                context.getString(R.string.toast_cut, selectedText),
                Toast.LENGTH_SHORT).show();
    }

    public static void paste(Context context, Calculator calc) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;

        String textToPaste = clip.getItemAt(0).coerceToText(context).toString();
        Toast.makeText(context,
                context.getString(R.string.toast_pasted, textToPaste),
                Toast.LENGTH_SHORT).show();
        calc.pasteIntoExpression(textToPaste);
    }
}
