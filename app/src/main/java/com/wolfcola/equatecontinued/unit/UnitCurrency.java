package com.wolfcola.equatecontinued.unit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.GregorianCalendar;

public class UnitCurrency extends Unit {
    public static final String DEFAULT_CURRENCY = "USD";
    private static final String JSON_LAST_UPDATE = "updated";
    private static final String JSON_FRACTION_VALUE = "fraction";
    private static final String JSON_FRACTION_PARENT = "parent";
    // default to no parent
    private static final String NO_PARENT = "no_parent";
    private String mParentCurrency = NO_PARENT;
    private double mFraction = 0.0;

    private Date mTimeLastUpdated;

    /**
     * Create a new currency unit
     *
     * @param name     abbreviated name of the currency (eg, "USD" for US dollar)
     * @param longName long name (eg, "US Dollar")
     * @param value    the price of the unit in inverted US dollars
     */
    public UnitCurrency(String name, String longName, double value) {
        super(name, longName, value);
        //Note that Jan = 0 in the Gregorian Calendar constructor below
        mTimeLastUpdated = new GregorianCalendar(2015, 3, 1, 1, 11).getTime();
    }

    /**
     * Create a new currency unit
     *
     * @param name       abbreviated name of the currency (eg, "USD" for US dollar)
     * @param longName   long name (eg, "US Dollar")
     * @param value      the price of the unit in inverted US dollars
     * @param updateTime the time the price was updated
     */
    public UnitCurrency(String name, String longName, double value,
                        GregorianCalendar updateTime) {
        super(name, longName, value);
        mTimeLastUpdated = updateTime.getTime();
    }

    /**
     * Create a new currency unit that is used for currency units that are
     * fractional sizes of other existing currencies.
     *
     * @param name       abbreviated name of the currency (eg, "USD" for US dollar)
     * @param longName   long name (eg, "US Dollar")
     * @param value      the price of the unit in inverted US dollars
     * @param updateTime the time the price was updated
     * @param parent     the symbol for the currency for which this is a
     *                   fraction of (for cents, this would be USD)
     * @param fraction   actual fraction of the parent currency
     */
    public UnitCurrency(String name, String longName, double value,
                        GregorianCalendar updateTime, String parent,
                        double fraction) {
        this(name, longName, value, updateTime);
        mParentCurrency = parent;
        mFraction = fraction;
    }

    /**
     * Load in the update time
     */
    @Override
    public boolean loadJSON(JSONObject json) throws JSONException {
        boolean success = super.loadJSON(json);
        //only load in the time if the JSON object matches this UNIT
        if (success) {
            setUpdateDate(new Date(json.getLong(JSON_LAST_UPDATE)));
            mFraction = json.getDouble(JSON_FRACTION_VALUE);
            mParentCurrency = json.getString(JSON_FRACTION_PARENT);
        }
        return success;
    }

    /**
     * Save the update time
     */
    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = super.toJSON();
        json.put(JSON_LAST_UPDATE, mTimeLastUpdated.getTime());
        json.put(JSON_FRACTION_VALUE, mFraction);
        json.put(JSON_FRACTION_PARENT, mParentCurrency);
        return json;
    }

    public Date getUpdateDate() {
        return mTimeLastUpdated;
    }

    public void setUpdateDate(Date date) {
        mTimeLastUpdated = date;
    }

    public long getTimeOfUpdate() {
        if (mTimeLastUpdated != null)
            return mTimeLastUpdated.getTime();
        else
            return 0;
    }


    @Override
    public String convertTo(Unit toUnit, String expressionToConvert) {
        return expressionToConvert + "*" + toUnit.getValue() + "/" + getValue();
    }

    public boolean isFractionCurrency() {
        return !mParentCurrency.equals(NO_PARENT);
    }

    public String getFractionParent() {
        return mParentCurrency;
    }

    /**
     * Update the value of this fractional currency with the value of the parent
     * currency, presumably after the parent has been updated
     */
    public void updateFractionalValue(double parent) {
        if (isFractionCurrency()) {
            setValue(mFraction * parent);
        }
    }
}

