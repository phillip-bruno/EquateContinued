package com.wolfcola.equatecontinued.view.IdlingResource;

/**
 * Release stand-in for the debug Espresso {@code SimpleIdlingResource}, so the
 * Espresso idling-resource library isn't shipped in the release APK.
 */
public class SimpleIdlingResource {
    public void setIdleState(boolean isIdleNow) {
    }
}
