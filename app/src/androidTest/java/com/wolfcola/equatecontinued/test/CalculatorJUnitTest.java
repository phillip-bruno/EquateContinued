package com.wolfcola.equatecontinued.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.res.Resources;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.wolfcola.equatecontinued.Calculator;
import com.wolfcola.equatecontinued.R;
import com.wolfcola.equatecontinued.Solver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.MathContext;

@RunWith(AndroidJUnit4.class)
public class CalculatorJUnitTest {

    // Precision for display and calculations
    public static final int INT_DISPLAY_PRECISION = 15;
    public static final int INT_CALC_PRECISION = INT_DISPLAY_PRECISION + 2;
    public String[] allKeyArray = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "+", "-", "*", "/", "b", "E", "="};
    private MathContext mcDisp = new MathContext(INT_DISPLAY_PRECISION);
    private Calculator bruteCalc;
    private String[] someKeyArray = {"0", "9", ".", "+", "-", "*", "/", "b", "E", "="};
    //these configure the testing
    private String[] testKeyArray = someKeyArray;

    /**
     * Returns a test Calculator instance with a mocked Resources object.
     * We use Mockito to simulate the resource array corresponding to
     * R.array.unit_type_array_combined.
     *
     * @return a test Calculator instance
     */
    static Calculator getTestCalc() {
        Resources mockResources = Mockito.mock(Resources.class);

        // Assuming R.array.unit_type_array_combined exists and is defined in your resources.
        int resourceId = R.array.unit_type_array_combined;
        String[] unitTypeArray = new String[]{
                "key_currency|Currency|Currency",
                "key_temp|Temperature|Temp",
                "key_weight|Weight|Weight",
                "key_len|Length|Length",
                "key_area|Area|Area",
                "key_vol|Volume|Volume",
                "key_speed|Speed|Speed",
                "key_time|Time|Time",
                "key_fuel|FuelEconomy|FuelEco",
                "key_power|Power|Power",
                "key_energy|Energy|Energy",
                "key_force|Force|Force",
                "key_torque|Torque|Torque",
                "key_pressure|Pressure|Pressure",
                "key_digital|DigitalStorage|Digital"
        };

        Mockito.when(mockResources.getStringArray(resourceId)).thenReturn(unitTypeArray);
        return Calculator.getTestCalculator(mockResources);
    }

    @Test
    public void sampleTest() {
        Calculator calc = getTestCalc();
        // A simple assertion to confirm our Calculator instance is not null.
        assertNotNull("Expected non-null Calculator instance.", calc);
        // Additional test cases here...
    }

    @Before
    public void setUp() throws Exception {
        // Place your setup code here
    }

    @After
    public void tearDown() throws Exception {
        // Place your cleanup code here
    }

    @Test
    public void testSomething() {
        // Your test code here
    }

    public void testParseKeyPressed() {
        Calculator calc = getTestCalc();
        //try all ops before (should do nothing), try double decimal, try changing op, and extra equals
        loadStringToCalc("=++/-+-*--1..+4.34b+-2-=", calc);

        assertEquals("1.3", calc.toString());

        //should clear the expression
        loadStringToCalc("b", calc);
        assertEquals("", calc.toString());


        //test clear key
        loadStringToCalc(".01=5c", calc);
        assertEquals("", calc.toString());

        //order of operations
        loadStringToCalc("10+.5*4=", calc);

        assertEquals("12", calc.toString());

        //try adding to expression
        loadStringToCalc("-4=", calc);
        assertEquals("8", calc.toString());

        //try typing a number now
        loadStringToCalc("4", calc);
        assertEquals("4", calc.toString());

        //try adding just a . make sure it does't break
        loadStringToCalc("1+.+2=", calc);
        assertEquals("41.2", calc.toString());


        //try adding just a .E and E. make sure it does't break
        loadStringToCalc(".1E.*/0E.E.-3=", calc);
        assertEquals("-2.9", calc.toString());

        //try to break it some more
        loadStringToCalc("4.E=1+.+E=.=", calc);

        //this had problems before...
        loadStringToCalc("0-5-5-5=", calc);

        //this had problems before...
        loadStringToCalc("1+E+1=", calc);

        loadStringToCalc("b-1+4=", calc);
        assertEquals("3", calc.toString());

        loadStringToCalc("3-1*-1=", calc);
        assertEquals("4", calc.toString());

        loadStringToCalc("b-3*-1*-1=", calc);
        assertEquals("-3", calc.toString());

        loadStringToCalc("3-(2-4)*-5=", calc);
        assertEquals("-7", calc.toString());

        loadStringToCalc("2+-2*-3=", calc);
        assertEquals("8", calc.toString());

        loadStringToCalc("(30+3%", calc);
        calc.setSelection(2, 2);
        loadStringToCalc("=", calc);
        assertEquals("30.03", calc.toString());
    }

    public void testNumberAccuracy() {
        Calculator calc = getTestCalc();
        loadStringToCalc("4", calc);

        //make sure 2.2 is represented properly
        loadStringToCalc("c2.2*3=", calc);
        assertEquals("6.6", calc.toString());

        //make sure 1/3= then *3 is 1
        loadStringToCalc("1/3=*3=", calc);
        assertEquals("1", calc.toString());

        //make #E+# and #E-# are parsed correctly (save time with constructor)
        calc = getTestCalc();
        loadStringToCalc("10000000000*10000000000+10=", calc);
        //be sure the exponent at the end is a 20 (other part of the string might be rounded differently
        assertTrue(calc.toString().matches(".*E20$"));
    }

    public void testErrors() {
        Calculator calc = getTestCalc();
        //divide by zero error
        loadStringToCalc("1/0=", calc);
        assertEquals(Solver.strDivideZeroError, calc.toString());

        //make sure num clears the error
        loadStringToCalc("+1+5=", calc);
        assertEquals("6", calc.toString());

        //overflow
        loadStringToCalc("9E9999999999=", calc);
    }

    public void testCleaning() {
        Calculator calc = getTestCalc();
        //make sure we're cleaning properly
        loadStringToCalc("c6.10000==", calc);
        assertEquals("6.1", calc.toString());
        loadStringToCalc("c0.00800==", calc);
        assertEquals("0.008", calc.toString());
        loadStringToCalc("c6.000==", calc);
        assertEquals("6", calc.toString());
        loadStringToCalc("c800==", calc);
        assertEquals("800", calc.toString());
        loadStringToCalc("c.080800==", calc);
        assertEquals("0.0808", calc.toString());
    }

    public void testExponents() {
        Calculator calc = getTestCalc();
        //first try to break it
        loadStringToCalc("E6EE*/bE++--*2=", calc);
        assertEquals("0.06", calc.toString());

        //try some random math
        loadStringToCalc("c30E2-2E10*=", calc);
        assertEquals("-19999997000", calc.toString());

        //tests to make sure that after a #.#E# expression, we can put a .
        loadStringToCalc("3.2E2*.5=", calc);
        assertEquals("160", calc.toString());


        //test conversion of long numbers into and out of E
        loadStringToCalc("6", calc);
        for (int i = 0; i < Calculator.DISPLAY_PRECISION; i++)
            loadStringToCalc("0", calc);
        loadStringToCalc("=", calc);
        //not sure if we'll be keeping the . after the 6, both will pass for now
        assertTrue(calc.toString().matches("6\\.?E" + Calculator.DISPLAY_PRECISION));

        //make sure the number one less than the precision is display as plain text
        String tester = "6";
        loadStringToCalc("c" + tester, calc);
        for (int i = 0; i < Calculator.DISPLAY_PRECISION - 1; i++) {
            loadStringToCalc("0", calc);
            tester = tester + "0";
        }
        //add plus 0 to spoof the equals toggle sci note
        loadStringToCalc("+0=", calc);
        assertEquals(tester, calc.toString());


        //0E8 should reduce to 0 not "E8"
        loadStringToCalc("0E8=", calc);
        assertEquals("0", calc.toString());

        //catch the potential problem of "532E+-"
        loadStringToCalc("c--5232E+-0=", calc);
        assertEquals("-5232", calc.toString());

        //catch problem where this would hang
        loadStringToCalc("1E9999999+1=", calc);
        assertEquals("Number Too Large", calc.toString());

        //catch problem where this would hang
        loadStringToCalc("1E9999999+1E999999=", calc);
        assertEquals("Number Too Large", calc.toString());

        loadStringToCalc("2E-2E-2=", calc);
        assertEquals("-1.98", calc.toString());

        //catch problem where this would hang the calculator if trying to print in plain text
        // we just want it to look sci instead
        loadStringToCalc("8E888=", calc);
        assertEquals("8E888", calc.toString());

        loadStringToCalc("c8E24=", calc);
        assertEquals("8000000000000000000000000", calc.toString());
    }

    public void testPower() {
        Calculator calc = getTestCalc();
        //test basic functionality
        loadStringToCalc("4^3=", calc);
        assertEquals("64", calc.toString());
        //test sqrt
        loadStringToCalc("64^.5=", calc);
        assertEquals("8", calc.toString());
        //test order of operations
        loadStringToCalc("2*3^(1+3)=", calc);
        assertEquals("162", calc.toString());
        //test lots of decimals
        loadStringToCalc("4.3^.3=", calc);
        BigDecimal bd = new BigDecimal(1.5489611908722423119058589800223, mcDisp);
        assertTrue(calc.toString().matches(bd.toString()));
        //test large numbers
        loadStringToCalc("9.1^500=", calc);
        assertEquals("Number Too Large", calc.toString());
        //test mixed exponents and powers
        loadStringToCalc("2.1E2^1.1E2=", calc);
        assertEquals("2.78049693531908E255", calc.toString());

        loadStringToCalc("3-3^2=", calc);
        assertEquals("-6", calc.toString());

        loadStringToCalc("b-2*-4^2=", calc);
        assertEquals("32", calc.toString());

        loadStringToCalc("(-5)^2.0=", calc);
        assertEquals("25", calc.toString());

        loadStringToCalc("b-(6)^(.9+(1.1))=", calc);
        assertEquals("-36", calc.toString());

        loadStringToCalc("(-2+1)^2=", calc);
        assertEquals("1", calc.toString());

        loadStringToCalc("3*-((-2)+(1^0))^2=", calc);
        assertEquals("-3", calc.toString());

        loadStringToCalc("3*-2.E-3^-2.E-3=", calc);
        assertEquals("-3.0375203397704", calc.toString());

        loadStringToCalc("2+-1^2*-9^((.5)^1+.1+-.1^1)=", calc);
        assertEquals("5", calc.toString());

        //TODO do we really wanna fix this?
        //loadStringToCalc("2^3^2=", calc);
        //assertEquals("512", calc.toString());

    }

    public void testSelection() {
        Calculator calc = getTestCalc();

        loadStringToCalc("342+-23523*3532", calc);
        assertEquals("342+-23523*3532", calc.toString());

        //this is reversed intentionally, this can happen if user drags end before start
        calc.setSelection(10, 5);
        loadStringToCalc("15", calc);
        assertEquals("342+-15*3532", calc.toString());

        calc.setSelection(5, 7);
        loadStringToCalc("-*1)", calc);
        calc.setSelection(0, 0);
        loadStringToCalc("(", calc);
        assertEquals("(342*1)*3532", calc.toString());

        calc.setSelection(7, 7);
        loadStringToCalc(".7", calc);
        assertEquals("(342*1)*.7*3532", calc.toString());

        calc.setSelection(11, 11);
        loadStringToCalc("^", calc);
        assertEquals("(342*1)*.7^3532", calc.toString());

        calc.setSelection(4, 11);
        loadStringToCalc("(", calc);
        assertEquals("(342*(3532", calc.toString());

        loadStringToCalc("=", calc);
        assertEquals("1207944", calc.toString());

        calc.setSelection(4, 4);
        loadStringToCalc("EE^*/-", calc);
        assertEquals("1207E-944", calc.toString());

        calc.setSelection(0, 9);
        loadStringToCalc("-8^3=", calc);
        assertEquals("-512", calc.toString());

        //TODO add this test to android test since this problem is fixed, it's just
        //fixed using an android specific method
        calc.setSelection(2, 2);
        calc.setSolved(false);
        loadStringToCalc(".", calc);
        assertEquals("-5.12", calc.toString());
    }


//	public void testUnits(){
//		Calculator calc = getTestCalc();
//
//		clickConvKey(Const.TEMP, Const.F, calc);
//		loadStringToCalc("212", calc);
//		clickConvKey(Const.TEMP, Const.C, calc);
//		assertEquals("100", calc.toString());
//		clickConvKey(Const.TEMP, Const.F, calc);
//		assertEquals("212", calc.toString());
//		clickConvKey(Const.TEMP, Const.K, calc);
//		assertEquals("373.15", calc.toString());
//		clickConvKey(Const.TEMP, Const.C, calc);
//		assertEquals("100", calc.toString());
//
//		//1E900 yard to mm to yard should not hang
//		loadStringToCalc("c1E900", calc);
//		clickConvKey(Const.LENGTH, Const.YARD, calc);
//		clickConvKey(Const.LENGTH, Const.MM, calc);
//		assertEquals("9.144E902", calc.toString());
//
//		//501 in F to K, check 533.70556, back to F should be 501
//		loadStringToCalc("c501", calc);
//		clickConvKey(Const.TEMP, Const.F, calc);
//		clickConvKey(Const.TEMP, Const.K, calc);
//		clickConvKey(Const.TEMP, Const.F, calc);
//		assertEquals("501", calc.toString());
//
//		loadStringToCalc("c456+3+6", calc);
//		calc.setSelection(5,5);
//		loadStringToCalc("b", calc);
//		clickConvKey(Const.TEMP, Const.F, calc);
//		clickConvKey(Const.TEMP, Const.K, calc);
//		assertEquals(Solver.strSyntaxError, calc.toString());
//	}

    public void testNegateOperator() {
        Calculator calc = getTestCalc();

        loadStringToCalc("-1+2+23+63n=", calc);
        assertEquals("-39", calc.toString());

        loadStringToCalc("5-7n=", calc);
        assertEquals("12", calc.toString());

        loadStringToCalc("n", calc);
        assertEquals("-12", calc.toString());

        loadStringToCalc("n", calc);
        assertEquals("12", calc.toString());

        loadStringToCalc("c(45-67)n1=", calc);
        assertEquals("-23", calc.toString());

        loadStringToCalc("43*n=", calc);
        assertEquals("43", calc.toString());

        loadStringToCalc("43E-30n==", calc);
        assertEquals("-4.3E-29", calc.toString());

        loadStringToCalc("c(-45n)=", calc);
        assertEquals("45", calc.toString());
    }

    public void testInvertOperator() {
        Calculator calc = getTestCalc();

        loadStringToCalc("-4i=", calc);
        assertEquals("-0.25", calc.toString());

        loadStringToCalc("i", calc);
        assertEquals("-4", calc.toString());

        loadStringToCalc("15+100+32", calc);
        calc.setSelection(3, 3);
        loadStringToCalc("i=", calc);
        assertEquals("47.01", calc.toString());

        loadStringToCalc("15+100+32", calc);
        calc.setSelection(6, 6);
        loadStringToCalc("i=", calc);
        assertEquals("47.01", calc.toString());

        loadStringToCalc("15+100+32", calc);
        calc.setSelection(4, 5);
        loadStringToCalc("i=", calc);
        assertEquals("47.01", calc.toString());

        loadStringToCalc("ci16=", calc);
        assertEquals("0.0625", calc.toString());

    }

    public void testPara() {
        Calculator calc = getTestCalc();

        loadStringToCalc("(2+3)*3+1*(3-1*(1))=", calc);
        assertEquals("17", calc.toString());

        loadStringToCalc("1+2*(2+3.3)=", calc);
        assertEquals("11.6", calc.toString());

        //test for adding multiplies between num and (
        loadStringToCalc("6(.1+.4)=", calc);
        assertEquals("3", calc.toString());

        //test for adding multiplies between . and (
        loadStringToCalc("2.(3)=", calc);
        assertEquals("6", calc.toString());

        //test for adding multiplies between E and (
        loadStringToCalc("5E(3*2)=", calc);
        assertEquals("5000000", calc.toString());

        //test for adding multiplies between E and (
        loadStringToCalc("5E(-3*2)=", calc);
        assertEquals("0.000005", calc.toString());

        //test for adding multiplies between E and (
        loadStringToCalc("(1-7)(2+1)=", calc);
        assertEquals("-18", calc.toString());

        //test for adding multiplies number and )
        loadStringToCalc("(2/4)10=", calc);
        assertEquals("5", calc.toString());

//		//test auto add opening para
//		loadStringToCalc("1+2+3)=", calc);
//		assertEquals("6", calc.toString());

        //test auto add closing para; also test para followed by invalid op
        loadStringToCalc("(*3=", calc);
        assertEquals("3", calc.toString());

        //test auto add closing para; also test para followed by invalid op
        loadStringToCalc("(3(.2=", calc);
        assertEquals("0.6", calc.toString());

        //test auto adding multiplies for paras
        loadStringToCalc("2((5)6)(3)7(3)(4).2+8.(0)=", calc);
        assertEquals("3024", calc.toString());
    }

    public void testPercent() {
        Calculator calc = getTestCalc();

        loadStringToCalc("200+5%=", calc);
        assertEquals("210", calc.toString());

        loadStringToCalc("200-5%=", calc);
        assertEquals("190", calc.toString());

        loadStringToCalc("200+-5%=", calc);
        assertEquals("190", calc.toString());

        loadStringToCalc("5%=", calc);
        assertEquals("0.05", calc.toString());

        loadStringToCalc("b-5%=", calc);
        assertEquals("-0.05", calc.toString());

        loadStringToCalc("200*5%=", calc);
        assertEquals("10", calc.toString());

        loadStringToCalc("200/5%=", calc);
        assertEquals("4000", calc.toString());

        loadStringToCalc("200*-5%=", calc);
        assertEquals("-10", calc.toString());

        loadStringToCalc("200/-5%=", calc);
        assertEquals("-4000", calc.toString());

        loadStringToCalc("1+200+5%=", calc);
        assertEquals("211.05", calc.toString());

        loadStringToCalc("1+200+5%*/^+-4%=", calc);
        assertEquals("202.608", calc.toString());

        loadStringToCalc("5%*%6=", calc);
        assertEquals("0.3", calc.toString());

        //test 88 percent of 5
        loadStringToCalc("88%5=", calc);
        assertEquals("4.4", calc.toString());

        //TODO, simplified broken version  is 1+2%3
        loadStringToCalc(".1+2.%3.%47.+200%.5=", calc);
        assertEquals("1.1282", calc.toString());
    }

    private void clickConvKey(int unitTypePos, int convKeyPos, Calculator calc) {
        calc.setCurrentUnitTypePos(unitTypePos);

        boolean requestConvert = calc.getCurrUnitType().selectUnit(convKeyPos);

        //this is normally performed in the convert key fragment, have to do this manually here
        if (requestConvert) {
            calc.convertFromTo(calc.getCurrUnitType().getPrevUnit(), calc.getCurrUnitType().getCurrUnit());
        }
    }

    /**
     * Helper function to type in keys to calc and return result
     *
     * @param str is input key presses
     */
    private void loadStringToCalc(String str, Calculator calc) {

        for (int i = 0; i < str.length(); i++) {

            calc.parseKeyPressed(String.valueOf(str.charAt(i)));
            //be sure to update where keys are going
            //	calc.setSelection(calc.toString().length(), calc.toString().length());
        }
    }

    //run the brute force test
    public void testBrute() {
        bruteCalc = getTestCalc();
        int numRuns = 4;
        bruteForceTest(numRuns, "");
    }


    //this will cycle through all combinations of keys
    private void bruteForceTest(int numTimes, String startSting) {
        if (numTimes == 0)
            return;
        numTimes--;
        for (int i = 0; i < testKeyArray.length; i++) {
            if (numTimes == 0) {
                for (int s = 0; s < startSting.length(); s++) {
                    String str = String.valueOf(startSting.charAt(s));
                    //System.out.print(str);
                    //System.out.println("Expression= " + bruteCalc.toString());

                    bruteCalc.parseKeyPressed(str);
                }
                try {
                    bruteCalc.parseKeyPressed("=");
                } catch (Exception e) {
                    //System.out.println("Error input: \"");
                    //System.out.print(startSting);
                    //System.out.println("=" + "\"");
                    e.printStackTrace();
                    throw new IllegalStateException();
                }
                //boolean clearAfterEach = true;
                //if(clearAfterEach)
                bruteCalc.parseKeyPressed("c");
                //System.out.println("=");
                break;
            } else
                bruteForceTest(numTimes, startSting + testKeyArray[i]);
        }
    }

}

















