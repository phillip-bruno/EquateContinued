package com.wolfcola.equatecontinued.test;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.ListView;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.viewpager.widget.ViewPager;

import com.wolfcola.equatecontinued.R;
import com.wolfcola.equatecontinued.test.IdlingResource.ViewPagerIdlingResource;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Set of utilities used to help perform Espresso tests
 */
public class EspressoTestUtils {
    public static void setUp(MyActivityTestRule activityTestRule) {
        // make sure Espresso hold long clicks for enough time
        // if this fails, make sure Settings -> Accessibility -> Touch & hold delay
        // is set to medium or long (for CircleCI)
        long timeEspressoHoldsKey = (long) (ViewConfiguration.getLongPressTimeout());
        long buttonLongTimeout = activityTestRule.getActivity()
                .getResources().getInteger(R.integer.long_click_timeout_test);
        assertThat(timeEspressoHoldsKey, is(greaterThanOrEqualTo(buttonLongTimeout)));
    }

    public static ViewPagerIdlingResource getPagerIdle(MyActivityTestRule activityTestRule) {
        // register an idling resource that will wait until a page settles before
        // doing anything next (such as clicking a unit within it)
        ViewPager vp = (ViewPager) activityTestRule.getActivity()
                .findViewById(com.wolfcola.equatecontinued.R.id.unit_pager);
        return new ViewPagerIdlingResource(vp, "unit_pager");
    }

    public static void assertResultPreviewInvisible() {
        onView(withId(R.id.resultPreview)).check(matches(
                withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    public static void assertResultPreviewEquals(String expected) {
        onView(withId(R.id.resultPreview)).check(matches(allOf(isDisplayed(),
                withText(expected))));
    }

    public static void assertExpressionEquals(String expected) {
        getTextDisplay().check(matches(expressionEquals(expected)));
    }

    /**
     * Method to check an expression contains the text given by the testString
     * parameter.  This method also checks the test string isn't null and to turn
     * it into a Matcher<String>.
     *
     * @param testString is the string to check the expression against
     * @return a Matcher<View> that can be used to turn into a View Interaction
     */
    private static Matcher<View> expressionEquals(String testString) {
        // use precondition to fail fast when a test is creating an invalid matcher
        checkArgument(!(testString.equals(null)));
        return expressionEquals(is(testString));
    }

    /**
     * Note that ideal the method below should implement a describeMismatch
     * method (as used by BaseMatcher), but this method is not invoked by
     * ViewAssertions.matches() and it won't get called. This means that I'm not
     * sure how to implement a custom error message.
     */
    private static Matcher<View> expressionEquals(final Matcher<String> testString) {
        return new BoundedMatcher<View, EditText>(EditText.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with expression text: " + testString);
            }

            @Override
            protected boolean matchesSafely(EditText item) {
                return testString.matches(item.getText().toString());
            }
        };
    }

    private static ViewInteraction getTextDisplay() {
        return onView(withId(R.id.textDisplay));
    }

    /**
     * Clicks on the tab for the provided Unit Type name. Note that the Unit Type
     * doesn't need to be visible.
     */
    public static void selectUnitTypeDirect(final String unitTypeName) {
        onView(allOf(withText(unitTypeName))).perform(
                new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return isEnabled(); // no constraints, they are checked above
                    }

                    @Override
                    public String getDescription() {
                        return "click unit type" + unitTypeName;
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        view.performClick();
                    }
                }
        );
    }

    /**
     * Clicks a unit in the unit pager with the displayed name of unitName
     */
    public static void clickUnit(String unitName) {
        onView(allOf(anyOf(withText(unitName), withText("➜ " + unitName)),
                isDescendantOfA(withId(R.id.unit_pager))
                , isDisplayed())).perform(click());
    }

    /**
     * Long clicks a unit in the unit pager with the displayed name of unitName
     */
    public static void longClickUnit(String unitName) {
        onView(allOf(anyOf(withText(unitName), withText("➜ " + unitName)),
                isDescendantOfA(withId(R.id.unit_pager))
                , isDisplayed())).perform(longClick());
    }

    /**
     * Checks that the unit button is visible for the given string
     */
    public static void checkUnitButtonVisibleWithArrow(String buttonText) {
        checkUnitButtonVisible("➜ " + buttonText);
    }

    /**
     * Checks that the unit button is visible for the given string
     */
    public static void checkUnitButtonVisible(String buttonText) {
        onView(allOf(withText(buttonText), isDescendantOfA(withId(R.id.unit_pager))))
                .check(matches(isDisplayed()));
    }


    public static void resetCalculator() {
        longClickButton("C");
        onView(withText("Calculator factory reset")).perform(click());
        onView(withText("OK")).perform(click());
    }

    private static void longClickButton(String s) {
        clickButton(s, true);
    }


    public static void clickButtons(String buttonString) {
        for (int i = 0; i < buttonString.length(); i++) {
            String s = buttonString.substring(i, i + 1);
            switch (s) {
                case "a":
                    i++;
                    clickPrevAnswer(Integer.parseInt(buttonString.substring(i, i + 1)));
                    break;
                case "q":
                    i++;
                    clickPrevQuery(Integer.parseInt(buttonString.substring(i, i + 1)));
                    break;
                default:
                    clickButton(s, false);
                    break;
            }
        }
    }

    private static void clickButton(String s, boolean longClick) {
        int id;

        //special case buttons
        switch (s) {
            case "^":
                id = R.id.multiply_button;
                longClick = true;
                break;
            case "E":
                id = R.id.percent_button;
                longClick = true;
                break;
            default:
                id = getButtonID(s);
        }

        if (longClick)
            onView(withId(id)).perform(longClick());
        else
            onView(withId(id)).perform(click());
    }

    /**
     * Helper function takes a string of a key hit and passes back the View id
     *
     * @param s plain text form of the button
     * @return id of the button
     */
    private static int getButtonID(String s) {
        int[] numButtonIds = {
                R.id.zero_button,
                R.id.one_button,
                R.id.two_button,
                R.id.three_button,
                R.id.four_button,
                R.id.five_button,
                R.id.six_button,
                R.id.seven_button,
                R.id.eight_button,
                R.id.nine_button};

        int buttonId = -1;

        switch (s) {
            case "+":
                buttonId = R.id.plus_button;
                break;
            case "-":
                buttonId = R.id.minus_button;
                break;
            case "*":
                buttonId = R.id.multiply_button;
                break;
            case "/":
                buttonId = R.id.divide_button;
                break;
            case ".":
                buttonId = R.id.decimal_button;
                break;
            case "=":
                buttonId = R.id.equals_button;
                break;
            case "%":
                buttonId = R.id.percent_button;
                break;
            case "^":
                buttonId = R.id.multiply_button;
                break;
            case "(":
                buttonId = R.id.open_para_button;
                break;
            case ")":
                buttonId = R.id.close_para_button;
                break;
            case "b":
                buttonId = R.id.backspace_button;
                break;
            case "C":
                buttonId = R.id.clear_button;
                break;
            default:
                //this for loop checks for numerical values
                for (int i = 0; i < 10; i++)
                    if (s.equals(Character.toString((char) (48 + i))))
                        buttonId = numButtonIds[i];
        }
        if (buttonId == -1) throw new InvalidButtonViewException(
                "No View could be found for button = \"" + s + "\"");
        return buttonId;
    }


    public static void clickPrevAnswer() {
        clickPrevAnswer(0);
    }

    public static void clickPrevAnswer(int position) {
        ResultClicker rc = new ResultClicker();
        rc.clickPrevAnswer(position);
    }

    public static void clickPrevQuery() {
        clickPrevQuery(0);
    }

    public static void clickPrevQuery(int position) {
        ResultClicker rc = new ResultClicker();
        rc.clickPrevQuery(position);
    }

    private static class ResultClicker {
        private int numberOfAdapterItems;

        public void clickPrevAnswer(int position) {
            updateNumberofResults();

            onData(anything())
                    .inAdapterView(withId(android.R.id.list))
                    .atPosition(numberOfAdapterItems - 1 - position)
                    .onChildView(withId(R.id.list_item_result_textPrevAnswer))
                    .perform(click());
        }

        public void clickPrevQuery(int position) {
            updateNumberofResults();

            onData(anything())
                    .inAdapterView(withId(android.R.id.list))
                    .atPosition(numberOfAdapterItems - 1 - position)
                    .onChildView(withId(R.id.list_item_result_textPrevQuery))
                    .perform(click());
        }

        private void updateNumberofResults() {
            onView(withId(android.R.id.list)).check(matches(new TypeSafeMatcher<View>() {
                @Override
                public boolean matchesSafely(View view) {
                    ListView listView = (ListView) view;
                    //here we assume the adapter has been fully loaded already
                    numberOfAdapterItems = listView.getAdapter().getCount();
                    return true;
                }

                @Override
                public void describeTo(Description description) {
                }
            }));
        }

    }

    private static class InvalidButtonViewException extends RuntimeException {
        InvalidButtonViewException(String message) {
            super(message);
        }
    }
}
