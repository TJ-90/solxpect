package org.woheller69.weather.weather_api.open_meteo;

import android.content.Context;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.text.TextUtils;

import org.woheller69.weather.BuildConfig;
import org.woheller69.weather.database.CityToWatch;
import org.woheller69.weather.preferences.AppPreferencesManager;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class OMHttpRequest {

    protected String getUrlForQueryingOMweatherAPI(Context context, float lat, float lon) {
        AppPreferencesManager prefManager =
                new AppPreferencesManager(PreferenceManager.getDefaultSharedPreferences(context));
        SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(context);

        return String.format(
                "%sforecast?latitude=%s&longitude=%s&forecast_days=%s&hourly=temperature_2m,diffuse_radiation,direct_normal_irradiance,weathercode&daily=weathercode,sunrise,sunset,&timeformat=unixtime&timezone=auto",
                BuildConfig.BASE_URL,
                lat,
                lon,
                sharedPreferences.getInt("pref_number_days",7)
        );

    }

}
