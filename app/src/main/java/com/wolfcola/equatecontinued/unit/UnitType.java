package com.wolfcola.equatecontinued.unit;

import android.content.Context;

import com.wolfcola.equatecontinued.unit.updater.UnitUpdater;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Abstract class, note that child class must implement a function to do raw
 * number conversion
 */
public class UnitType {
    private static final String JSON_NAME = "name";
    private static final String JSON_UNIT_DISP_ORDER = "unit_disp_order";
    private static final String JSON_CURR_UNIT_POS_IN_ARRAY = "pos";
    private static final String JSON_IS_SELECTED = "selected";
    private static final String JSON_UNIT_ARRAY = "unit_array";
    private static final String JSON_UPDATE_TIME = "update_time";

    private String mName;
    private ArrayList<Unit> mUnitArray;
    private Unit mPrevUnit;
    private Unit mCurrUnit;
    private boolean mIsUnitSelected;
    private boolean mContainsDynamicUnits = false;
    //Order to display units (based on mUnitArray index
    private ArrayList<Integer> mUnitDisplayOrder;

    // flag used to tell if the unit is currently being asynchronously updated
    private boolean mUpdating = false;
    private Date mLastUpdateTime;


    //this is for communication with fragment hosting convert keys
    private OnConvertKeyUpdateFinishedListener mCallback;


    /**
     * Default constructor used by UnitType Initializer
     */
    public UnitType(String name) {
        mName = name;
        mUnitArray = new ArrayList<>();
        mUnitDisplayOrder = new ArrayList<>();
        mIsUnitSelected = false;
        mUpdating = false;
        mLastUpdateTime = new GregorianCalendar(2015, 3, 1, 1, 11).getTime();
    }

    /**
     * Takes JSON object and loads out user saved info, such as currently
     * selected unit and unit display order
     *
     * @param json is the JSON object that contains save data of UnitType
     *             to load.
     */
    public void loadJSON(JSONObject json) throws JSONException {
        //Check to make we have the right saved JSON, else leave
        if (!getUnitTypeName().equals(json.getString(JSON_NAME)))
            return;

        //load in saved data from Units (currency values and update times)
        //note that no other type of unit is loaded
        if (containsDynamicUnits()) {
            JSONArray jUnitArray = json.getJSONArray(JSON_UNIT_ARRAY);
            for (int i = 0; i < jUnitArray.length(); i++) {
                getUnit(i).loadJSON(jUnitArray.getJSONObject(i));
            }
        }

        mCurrUnit = getUnitPosInUnitArray(json.getInt(JSON_CURR_UNIT_POS_IN_ARRAY));
        mIsUnitSelected = json.getBoolean(JSON_IS_SELECTED);
        setLastUpdateTime(new Date(json.getLong(JSON_UPDATE_TIME)));

        JSONArray jUnitDisOrder = json.getJSONArray(JSON_UNIT_DISP_ORDER);
        mUnitDisplayOrder.clear();
        for (int i = 0; i < jUnitDisOrder.length(); i++) {
            mUnitDisplayOrder.add(jUnitDisOrder.getInt(i));
        }
        //fill in the remaining if missing (if we added a unit)
        fillUnitDisplayOrder();
    }

    /**
     * Save the state of this UnitType into a JSON object for later use
     *
     * @return JSON object that contains this object
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();

        //should only be saving data from Currency unit type
        if (containsDynamicUnits()) {
            JSONArray jUnitArray = new JSONArray();
            for (Unit unit : mUnitArray)
                jUnitArray.put(unit.toJSON());
            json.put(JSON_UNIT_ARRAY, jUnitArray);
        }

        //used to identify this UnitType from others
        json.put(JSON_NAME, mName);
        json.put(JSON_CURR_UNIT_POS_IN_ARRAY, findUnitPosInUnitArray(mCurrUnit));
        json.put(JSON_IS_SELECTED, mIsUnitSelected);
        json.put(JSON_UPDATE_TIME, getLastUpdateTime().getTime());


        JSONArray jUnitDisOrder = new JSONArray();
        for (Integer i : mUnitDisplayOrder)
            jUnitDisOrder.put(i);
        json.put(JSON_UNIT_DISP_ORDER, jUnitDisOrder);

        return json;
    }

    /**
     * Used to build a UnitType after it has been created
     */
    public void addUnit(Unit u) {
        mUnitArray.add(u);
        //0th element is 0, 1st is 1, etc
        mUnitDisplayOrder.add(mUnitDisplayOrder.size());
        //if there was already a dynamic unit or this one is, UnitType still contains dynamic units
        if (mContainsDynamicUnits || u.isDynamic()) mContainsDynamicUnits = true;
    }

    /**
     * Swap positions of units
     */
    public void swapUnits(int pos1, int pos2) {
        Collections.swap(mUnitDisplayOrder, pos1, pos2);
    }

    /**
     * Rotates the units in the display order such that the unit at toIndex - 1
     * now has position fromIndex, the unit at fromIndex has position fromIndex +
     * 1, etc.
     *
     * @param fromIndex is inclusive
     * @param toIndex   is exclusive
     */
    public void rotateUnitSublist(int fromIndex, int toIndex) {
        Collections.rotate(mUnitDisplayOrder.subList(fromIndex, toIndex), 1);
    }

    /**
     * Sorts a sublist of units in display order by name
     *
     * @param fromIndex is inclusive
     * @param toIndex   is exclusive
     */
    public void sortUnitSublist(int fromIndex, int toIndex) {
        Collections.sort(mUnitDisplayOrder.subList(fromIndex, toIndex),
                new Comparator<Integer>() {
                    public int compare(Integer s1, Integer s2) {
                        return mUnitArray.get(s1).getLongName()
                                .compareTo(mUnitArray.get(s2).getLongName());
                    }
                });
    }

    /**
     * Find the position of the unit in the current unit button order
     *
     * @return -1 if selection failed, otherwise the position of the unit
     */
    public int findUnitButtonPosition(Unit unit) {
        if (unit == null)
            return -1;
        for (int i = 0; i < size(); i++) {
            if (unit.equals(getUnit(i)))
                return i; //found the unit
        }
        return -1;  //if we didn't find the unit
    }

    public int findUnitPosInUnitArray(Unit unit) {
        if (unit == null) return -1;
        for (int i = 0; i < size(); i++) {
            if (unit.equals(getUnitPosInUnitArray(i)))
                return i; //found the unit
        }
        return -1;  //if we didn't find the unit
    }

    /**
     * This function takes a unit array position (the original position of a unit
     * when the unit array is created in code) and returns the display order
     * position (after units have been moved around by the user.
     *
     * @param pos the position of the unit in the original unit array
     * @return the position of the unit in the display order array
     */
    public int findButtonPositionforUnitArrayPos(int pos) {
        return mUnitDisplayOrder.indexOf(pos);
    }

    /**
     * If mCurrUnit not set, set mCurrUnit
     * If mCurrUnit already set, call functions to perform a convert
     *
     * @return returns if a conversion is requested
     */
    public boolean selectUnit(int clickedButPos) {
        Unit unitPressed = getUnit(clickedButPos);

        //used to tell caller if we needed to do a conversion
        boolean requestConvert = false;
        //If we've already selected a unit, do conversion
        if (mIsUnitSelected) {
            //if the unit is the same as before, de-select it
            if (getCurrUnitButtonPos() == clickedButPos) {
                //if historical unit, allow selection again (for a different year)
                if (!unitPressed.isHistorical()) {
                    mIsUnitSelected = false;
                    return false;
                }
            }
            mPrevUnit = mCurrUnit;
            requestConvert = true;
        }

        //Select new unit regardless
        mCurrUnit = unitPressed;
        //Engage set flag
        mIsUnitSelected = true;
        return requestConvert;
    }

    /**
     * Update values of units that are not static (currency) via
     * each unit's own HTTP/JSON api call. Note that this refresh
     * is asynchronous and will only happen sometime in the future
     * Internet connection permitting.
     */
    public void updateDynamicUnits(Context c, boolean forced) {
        if (containsDynamicUnits()) {
            new UnitUpdater(c).update(this, forced);
        }
    }

    /**
     * Check to see if this UnitType holds any units that have values that
     * need to be refreshed via the Internet
     */
    public boolean containsDynamicUnits() {
        return mContainsDynamicUnits;
    }

    /**
     * Check to see if unit at position pos is dynamic
     */
    public boolean isUnitDynamic(int pos) {
        return getUnit(pos).isDynamic();
    }

//	/** Check to see if unit at position pos is currently updating */
//	public boolean isUnitUpdating(int pos){
//		//TODO have this return true if isUpdating is true, otherwise do the following
//		//TODO also make convert keys reflect this change (so all will show updating)
//		//TODO but once yahoo xml update is finished, individuals will show updating
//		if(getUnit(pos).isDynamic())
//			return ((UnitCurrency)getUnit(pos)).isUpdating();
//		else
//			return false;
//	}

    public void setDynamicUnitCallback(OnConvertKeyUpdateFinishedListener callback) {
        if (containsDynamicUnits()) {
            mCallback = callback;
//			for (int i = 0; i < size(); i++)
//				if (getUnit(i).isDynamic())
//					((UnitCurrency) getUnit(i)).setCallback(mCallback);
        }
    }

    /**
     * Check to see if unit at position pos is dynamic
     */
    public boolean isUnitHistorical(int pos) {
        return getUnit(pos).isHistorical();
    }

    /**
     * Resets mIsUnitSelected flag
     */
    public void clearUnitSelection() {
        mIsUnitSelected = false;
    }

    public boolean isUnitSelected() {
        return mIsUnitSelected;
    }

    public String getUnitTypeName() {
        return mName;
    }

    /**
     * @param pos is index of Unit in the mUnitArray list
     * @return String name to be displayed on convert button
     */
    public String getUnitDisplayName(int pos) {
        return getUnit(pos).toString();
    }

    public String getLowercaseGenericLongName(int pos) {
        return getUnit(pos).getLowercaseGenericLongName();
    }

    /**
     * Method builds charSequence array of long names of undisplayed units
     *
     * @param numDispUnits is Array of long names of units not being displayed
     * @return Number of units being displayed, used to find undisplayed units
     */
    public CharSequence[] getUndisplayedUnitNames(int numDispUnits) {
        //ArrayList<Unit> subList = mUnitArray.subList(numDispUnits, size());
        //return subList.toArray(new CharSequence[subLists.size()]);
        int arraySize = size() - numDispUnits;
        CharSequence[] cs = new CharSequence[arraySize];
        for (int i = 0; i < arraySize; i++) {
            cs[i] = getUnit(numDispUnits + i).getLongName();
        }
        return cs;
    }

    /**
     * Get a unit given a unit's abbreviation in the unit type array.
     * Abbreviations should be unique for each unit, otherwise unit buttons
     * could look the same.  Since abbreviations should be unique, we shouldn't
     * have early/false finds.
     *
     * @param abbreviation for the unit to find
     * @return the Unit that has the abbreviation supplied
     */
    public Unit getUnit(String abbreviation) {
        for (Unit u : mUnitArray) {
            if (u.getAbbreviation().equals(abbreviation)) {
                return u;
            }
        }
        return null;
    }

    /**
     * Used to get the Unit at a given position.  Note that the position is the
     * user defined order for buttons. Uses a LUT to convert displayed position
     * into real array position.
     *
     * @param buttonPos Position of unit to retrieve (user defined order)
     * @return Unit at the given position
     */
    public Unit getUnit(int buttonPos) {
        //If a unit is added
        if (mUnitDisplayOrder.size() < size())
            fillUnitDisplayOrder();
        //if somehow there are more UnitDisplayOrder (unit deleted), don't want
        //to address nonexistent element
        if (mUnitDisplayOrder.size() > size())
            resetUnitDisplayOrder();

        return mUnitArray.get(mUnitDisplayOrder.get(buttonPos));
    }

    /**
     * Get the unit given a position of the original mUnitArray.  Unlike getUnit,
     * this does not use the mUnitDisplayOrder. Will return null if pos is
     * invalid (less than 0 or greater than mUnitArray size)
     *
     * @param pos position of unit in mUnitArray
     * @return unit from mUnitArray
     */
    public Unit getUnitPosInUnitArray(int pos) {
        if (pos < 0 || pos >= mUnitArray.size()) return null;
        return mUnitArray.get(pos);
    }

    /**
     * Populate the UnitDisplayOrder array.  Will fill if empty or top off if
     * it has less elements than UnitArray
     */
    private void fillUnitDisplayOrder() {
        for (int i = mUnitDisplayOrder.size(); i < size(); i++)
            mUnitDisplayOrder.add(mUnitDisplayOrder.size());
    }

    private void resetUnitDisplayOrder() {
        mUnitDisplayOrder.clear();
        fillUnitDisplayOrder();
    }

    public Unit getPrevUnit() {
        return mPrevUnit;
    }

    public Unit getCurrUnit() {
        return mCurrUnit;
    }

    /**
     * Get the number of Units in this UnitType
     *
     * @return integer of number of Units in this UnitType
     */
    public int size() {
        return mUnitArray.size();
    }

    public int getCurrUnitButtonPos() {
        return findUnitButtonPosition(mCurrUnit);
    }

    /**
     * Returns the updating flag, which is used to signify whether or not this
     * unit type is currently being asynchronously updated.
     */
    public boolean isUpdating() {
        return mUpdating;
    }

    /**
     * Function used to set the updating flag, which is used to signify whether
     * or not this unit type is currently being asynchronously updated.
     *
     * @param updating is true if unit is being updated false otherwise
     */
    public void setUpdating(boolean updating) {
        mUpdating = updating;
        //refresh text
        if (mCallback != null)
            mCallback.refreshAllButtonsText();
    }

    public Date getLastUpdateTime() {
        return mLastUpdateTime;
    }

    public void setLastUpdateTime(Date mLastUpdateTime) {
        this.mLastUpdateTime = mLastUpdateTime;
    }

    public interface OnConvertKeyUpdateFinishedListener {
        void refreshAllButtonsText();
    }
}