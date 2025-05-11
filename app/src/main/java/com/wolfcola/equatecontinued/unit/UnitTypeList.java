package com.wolfcola.equatecontinued.unit;

import android.content.Context;
import android.content.res.Resources;

import com.wolfcola.equatecontinued.ResourceArrayParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Class to store and manage the array of Unit Types available to the calculator
 */

public class UnitTypeList {
    private static final String JSON_UNIT_TYPE_MAP = "unit_type_map";
    private static final String JSON_UNIT_TYPE_ORDER = "unit_type_order";
    private static final String JSON_UNIT_TYPE = "unit_type";
    private static final int DEFAULT_POS = 3;
    private final ArrayList<String> XML_KEYS;
    private final ArrayList<String> TAB_NAMES;

    //actually stores all of the Unit Type Objects
    private HashMap<String, UnitType> mUnitTypes;
    private ArrayList<String> mOrderedUnitKeys;

    //stores the current location in mUnitTypeArray
    private String mCurrentKey;

    public UnitTypeList(Resources resources) {
        XML_KEYS = ResourceArrayParser.getUnitTypeKeyArrayList(resources);

        TAB_NAMES = ResourceArrayParser.getUnitTypeTabNameArrayList(resources);

        //initialize storage members
        mUnitTypes = new HashMap<>();
        mOrderedUnitKeys = new ArrayList<>();

        //clear existing values and load in defaults
        initialize();
    }

    /**
     * Constructor used to build a new UnitTypeList with a JSON object. This is
     * used to recall saved Unit Type Arrays that might have different visibility,
     * order, or unit customizations from the user.
     *
     * @param json Object to load JSON from
     * @throws JSONException
     */
    public UnitTypeList(Resources resources, JSONObject json) throws JSONException {
        this(resources); // initialize unit array

        JSONArray jUnitTypeArray = json.getJSONArray(JSON_UNIT_TYPE_MAP);

        //if we added another UnitType and total count is different than previous
        // just use default
        if (jUnitTypeArray.length() == mUnitTypes.size()) {
            //Load in user settings to already assembled UnitType array
            int i = 0;
            for (String key : mUnitTypes.keySet()) {
                mUnitTypes.get(key).loadJSON(jUnitTypeArray.getJSONObject(i));
                i++;
            }

            //load out the array or keys that define the desired order
            JSONArray jUnitOrderArray = json.getJSONArray(JSON_UNIT_TYPE_ORDER);
            ArrayList<String> temp = new ArrayList<>();
            for (int k = 0; k < jUnitOrderArray.length(); k++) {
                temp.add(jUnitOrderArray.getString(k));
            }
            mOrderedUnitKeys = temp;

            //grab the current key
            mCurrentKey = json.getString(JSON_UNIT_TYPE);
        }
    }

    /**
     * Loads the Unit Type Array into a JSON array for use in parent class for
     * saving customizations.
     *
     * @return JSON array of Unit Types
     * @throws JSONException
     */
    public JSONObject toJSON() {
        JSONObject jsonReturnObj = new JSONObject();
        try {
            jsonReturnObj.put(JSON_UNIT_TYPE, getCurrentKey());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray jUnitTypeArray = new JSONArray();
        for (UnitType u : mUnitTypes.values())
            try {
                jUnitTypeArray.put(u.toJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }

        try {
            jsonReturnObj.put(JSON_UNIT_TYPE_MAP, jUnitTypeArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONArray jOrderedKeyArray = new JSONArray();
        for (String s : mOrderedUnitKeys)
            jOrderedKeyArray.put(s);

        try {
            jsonReturnObj.put(JSON_UNIT_TYPE_ORDER, jOrderedKeyArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonReturnObj;
    }


    /**
     * Helper method used to initiate the array of various types of units
     */
    public void initialize() {
        //set the unit type to length by default
        if (DEFAULT_POS < XML_KEYS.size())
            mCurrentKey = XML_KEYS.get(DEFAULT_POS);
        else
            mCurrentKey = XML_KEYS.get(0);

        mUnitTypes.clear();
        mUnitTypes = UnitInitializer.getUnitTypeMap(XML_KEYS, TAB_NAMES);

        mOrderedUnitKeys.clear();
        mOrderedUnitKeys = new ArrayList<>(XML_KEYS);
    }

    /**
     * Gets the key of the currency selected Unit Type
     */
    public String getCurrentKey() {
        return mCurrentKey;
    }

    /**
     * Get the Unit Type at the given key.
     *
     * @param key of Unit Type to get
     */
    public UnitType get(String key) {
        return mUnitTypes.get(key);
    }

    /**
     * Get the Unit Type at the given index of the currently visible Unit Types
     *
     * @param index of the visible Unit Type to get
     */
    public UnitType get(int index) {
        return mUnitTypes.get(mOrderedUnitKeys.get(index));
    }

    public HashMap<String, UnitType> getUnitTypeArray() {
        return mUnitTypes;
    }

    /**
     * Get the currently selected Unit Type
     */
    public UnitType getCurrent() {
        return get(getCurrentKey());
    }

    /**
     * Set the currently selected Unit Type
     */
    public void setCurrent(String key) {
        mCurrentKey = key;
    }

    /**
     * Set the currently selected Unit Type by index of currently visible Unit
     * Types
     */
    public void setCurrent(int index) {
        mCurrentKey = mOrderedUnitKeys.get(index);
    }

    public int numberVisible() {
        return mOrderedUnitKeys.size();
    }

    public int getIndex(String key) {
        return mOrderedUnitKeys.indexOf(key);
    }

    /**
     * Gets the index of the currently selected unit within the visible and
     * ordered Unit Types
     */
    public int getCurrentIndex() {
        int index = getIndex(getCurrentKey());
//		// we have some error with the current key, default to 0
//		if (index == -1) {
//			setCurrent(0);
//			index = 0;
//		}
        return index;
    }

    /**
     * Update values of units that are not static (currency) via
     * each unit's own HTTP/JSON API call. Note that this refresh
     * is asynchronous and will only happen sometime in the future
     * Internet connection permitting.
     *
     * @param forced should update be forced without waiting for time-out
     */
    public void refreshDynamicUnits(Context context, boolean forced) {
        for (UnitType ut : mUnitTypes.values())
            ut.updateDynamicUnits(context, forced);
    }

    public void setOrdered(Set<String> ordered) {
        if (ordered == null) {
            mOrderedUnitKeys.clear();
            mOrderedUnitKeys = new ArrayList<>(XML_KEYS);
        } else {
            mOrderedUnitKeys = new ArrayList<>(XML_KEYS);
            mOrderedUnitKeys.retainAll(ordered);
        }
    }
}
