package com.wolfcola.equatecontinued;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class ExpressionTest {
    private static final int PRECISION = 15;
    private Locale mOriginalLocale;

    @Before
    public void setUp() {
        mOriginalLocale = Locale.getDefault();
    }

    @After
    public void tearDown() {
        Locale.setDefault(mOriginalLocale);
    }

    @Test
    public void sciNotationIgnoresCommaDecimalLocale() {
        Locale.setDefault(Locale.GERMANY);
        Expression exp = new Expression(PRECISION);
        exp.replaceExpression("1500");
        exp.roundAndCleanExpression(Expression.NumFormat.SCI_NOTE);

        assertEquals("1.5E3", exp.toString());
        assertFalse(exp.isInvalid());
    }

    @Test
    public void copyDoesNotShareHighlightList() {
        Expression original = new Expression(PRECISION);
        original.getHighlighted().add(1);

        Expression copy = new Expression(original);
        copy.getHighlighted().add(2);
        copy.clearHighlightedList();

        assertEquals(1, original.getHighlighted().size());
    }

    @Test
    public void jsonRoundTripKeepsSolvedAndSelection() throws Exception {
        Expression exp = new Expression(PRECISION);
        exp.replaceExpression("12");
        exp.setSelection(1, 2);
        exp.setSolved(true);

        JSONObject json = exp.toJSON();
        assertEquals(2, json.getInt("sel_end"));

        assertTrue(new Expression(json, PRECISION).isSolved());
    }

    @Test
    public void jsonLoadsLegacySolvedFlag() throws Exception {
        //saves from before the key fix stored the solved flag under "sel_end"
        JSONObject legacy = new JSONObject()
                .put("expression", "12")
                .put("precise", "")
                .put("sel_start", 0)
                .put("sel_end", true);

        assertTrue(new Expression(legacy, PRECISION).isSolved());
    }
}
