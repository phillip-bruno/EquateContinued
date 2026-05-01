package com.wolfcola.equatecontinued.view;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.wolfcola.equatecontinued.Calculator;

/**
 * ViewModel wrapping the Calculator singleton, exposing observable UI state.
 * Survives configuration changes (rotation). Fragments and the Activity
 * obtain this via ViewModelProvider(activity) so they share the same instance.
 */
public class CalcViewModel extends AndroidViewModel {

    private final Calculator mCalc;

    // Whether the screen needs a full refresh (result list + display)
    private final MutableLiveData<Boolean> mUpdateScreen = new MutableLiveData<>();

    // Whether a unit is currently selected (drives equals button color)
    private final MutableLiveData<Boolean> mUnitSelected = new MutableLiveData<>(false);

    // Position of unit to select after a ViewPager scroll completes (-1 = none)
    private int mUnitPosToSelectAfterScroll = -1;

    public CalcViewModel(@NonNull Application application) {
        super(application);
        mCalc = Calculator.getCalculator(application);
    }

    public Calculator getCalc() {
        return mCalc;
    }

    /**
     * Called when any calculator button is pressed.
     *
     * @param keyPressed ASCII representation of the key ("1", "=", "*", etc)
     * @return the result flags from the Calculator
     */
    public Calculator.CalculatorResultFlags onButtonPressed(String keyPressed) {
        return mCalc.parseKeyPressed(keyPressed);
    }

    // --- Unit scroll state (replaces CalcActivity.unitPosToSelectAfterScroll) ---

    public int getUnitPosToSelectAfterScroll() {
        return mUnitPosToSelectAfterScroll;
    }

    public void setUnitPosToSelectAfterScroll(int pos) {
        mUnitPosToSelectAfterScroll = pos;
    }

    public void clearUnitPosToSelectAfterScroll() {
        mUnitPosToSelectAfterScroll = -1;
    }

    // --- Observable state ---

    public LiveData<Boolean> getUpdateScreen() {
        return mUpdateScreen;
    }

    public void requestScreenUpdate(boolean updateResult) {
        mUpdateScreen.setValue(updateResult);
    }

    public LiveData<Boolean> getUnitSelected() {
        return mUnitSelected;
    }

    public void setUnitSelected(boolean selected) {
        mUnitSelected.setValue(selected);
    }

    // --- Event: request to select a unit at a specific position ---

    private final MutableLiveData<UnitSelectEvent> mSelectUnitEvent = new MutableLiveData<>();

    public LiveData<UnitSelectEvent> getSelectUnitEvent() {
        return mSelectUnitEvent;
    }

    public void selectUnitAtUnitArrayPos(int unitPos, String unitTypeKey) {
        mSelectUnitEvent.setValue(new UnitSelectEvent(unitPos, unitTypeKey));
    }

    public record UnitSelectEvent(int unitPos, String unitTypeKey) {
    }
}
