package com.wolfcola.equatecontinued.unit.updater;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import com.wolfcola.equatecontinued.R;
import com.wolfcola.equatecontinued.unit.Unit;
import com.wolfcola.equatecontinued.unit.UnitCurrency;
import com.wolfcola.equatecontinued.unit.UnitType;
import com.wolfcola.equatecontinued.unit.updater.CurrencyURLParser.CurrencyParseException;
import com.wolfcola.equatecontinued.view.ViewUtils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Helper class is used to update dynamic unit types such as currency by
 * fetching the latest rates on a background thread
 * Created by Evan on 10/2/2015.
 */
public class UnitUpdater {
    private static final int UPDATE_TIMEOUT_MIN = 90;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    //application context, so a rotation mid-update doesn't leak the activity
    private final Context mContext;

    public UnitUpdater(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Perform an update of units inside a UnitType if the timeout period has
     * been reached or if the refresh is forced.
     */
    public void update(UnitType ut, boolean forced) {
        if (!ut.containsDynamicUnits()) return;

        //only do update if timeout period has passed
        if (isTimeoutReached(ut) || forced) {
            //add "Updating" text to each visible button
            ut.setUpdating(true);
            // perform the unit update using a separate thread so UI thread
            // doesn't get bogged down
            EXECUTOR.execute(() -> {
                ErrorCause error = isNetworkAvailable() ? updateRates(ut) : ErrorCause.NO_INTERNET;
                MAIN_HANDLER.post(() -> onUpdateFinished(ut, forced, error));
            });
        } else {
            ViewUtils.toast(mContext.getText(R.string.words_units_up_to_date)
                    .toString(), mContext);
        }
    }

    /**
     * Determines if the refresh timeout on the unit has been reached. Don't want
     * to spam the API with lots of requests.
     */
    private boolean isTimeoutReached(UnitType ut) {
        Date now = new Date();
        return !(ut.getLastUpdateTime() != null && (now.getTime() -
                ut.getLastUpdateTime().getTime())
                < (60L * 1000 * UPDATE_TIMEOUT_MIN));
    }

    /**
     * Runs on the main thread once the background update completes
     *
     * @param error null if the update succeeded
     */
    private void onUpdateFinished(UnitType ut, boolean forced, ErrorCause error) {
        if (error == null) {
            ut.setLastUpdateTime(new Date());
            CharSequence text = mContext.getText(R.string.update_at);
            if (forced) text = mContext.getText(R.string.update_forced);
            ViewUtils.toastLong(text + ut.getLastUpdateTime().toString(), mContext);
        } else {
            // something went wrong with the update, toast the user more info
            switch (error) {
                case NO_INTERNET:
                    ViewUtils.toastLong(R.string.update_error_internet, mContext);
                    break;
                case PARSING_ERROR:
                    ViewUtils.toastLong(R.string.update_error_parsing, mContext);
                    break;
                case TIMEOUT:
                    ViewUtils.toastLong(R.string.update_error_timeout, mContext);
                    break;
                default:
                    ViewUtils.toastLong(R.string.update_error_unknown, mContext);
                    break;
            }
        }

        //remove text "Updating"
        ut.setUpdating(false);
    }

    /**
     * Method to determine if the the device is connected to the internet
     *
     * @return true if the device is connected to the internet, false otherwise
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = mContext.getSystemService(ConnectivityManager.class);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * Download the latest rates and apply them to the currencies in ut.
     * Runs on a background thread.
     *
     * @return null on success, otherwise the reason the update failed
     */
    private ErrorCause updateRates(UnitType ut) {
        HashMap<String, CurrencyURLParser.Entry> currRates;

        try {
            currRates = new FloatRateParser().downloadAndParse();
            HashMap<String, CurrencyURLParser.Entry> cryptCurrRates =
                    new CoinpaprikaParser().downloadAndParse();

            // add the two set of rates together, but make sure the normal
            // currency rates will overwrite the crypto rates when there is a
            // symbol overlap
            cryptCurrRates.putAll(currRates);
            currRates = cryptCurrRates;
        } catch (CurrencyParseException e) {
            e.printStackTrace();
            return ErrorCause.PARSING_ERROR;
        } catch (IOException e) {
            e.printStackTrace();
            return ErrorCause.TIMEOUT;
        }

        //update each existing curr with new rates
        for (int i = 0; i < ut.size(); i++) {
            //skip unit support that don't support updating (not hist)
            if (!ut.getUnitPosInUnitArray(i).isDynamic()) continue;

            UnitCurrency u = ((UnitCurrency) ut.getUnitPosInUnitArray(i));
            CurrencyURLParser.Entry entry = currRates.get(u.getAbbreviation());
            if (entry != null) {
                u.setValue(entry.price());
                u.setUpdateDate(entry.date());
            }
        }

        //TODO inefficient to loop over UnitType twice. instead store fractional
        //TODO currencies in a separate array in UnitType perhaps?
        //update each fractional currency with new rates
        for (int i = 0; i < ut.size(); i++) {
            Unit unit = ut.getUnitPosInUnitArray(i);
            if (unit.isDynamic()) {
                UnitCurrency uc = (UnitCurrency) unit;
                if (uc.isFractionCurrency()) {
                    UnitCurrency parent = (UnitCurrency) ut.getUnit(uc.getFractionParent());
                    double parentValue = ut.getUnit(uc.getFractionParent()).getValue();
                    uc.updateFractionalValue(parentValue);
                    uc.setUpdateDate(parent.getUpdateDate());
                }
            }
        }
        return null;
    }

    private enum ErrorCause {NO_INTERNET, TIMEOUT, PARSING_ERROR}
}
