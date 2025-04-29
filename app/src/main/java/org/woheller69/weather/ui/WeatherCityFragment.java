package org.woheller69.weather.ui;

import static org.woheller69.weather.ui.RecycleList.CityWeatherAdapter.CHART;
import static org.woheller69.weather.ui.RecycleList.CityWeatherAdapter.DAY;
import static org.woheller69.weather.ui.RecycleList.CityWeatherAdapter.EMPTY;
import static org.woheller69.weather.ui.RecycleList.CityWeatherAdapter.OVERVIEW;
import static org.woheller69.weather.ui.RecycleList.CityWeatherAdapter.WEEK;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;

import org.woheller69.weather.R;
import org.woheller69.weather.activities.ForecastCityActivity;
import org.woheller69.weather.database.GeneralData;
import org.woheller69.weather.database.HourlyForecast;
import org.woheller69.weather.database.SQLiteHelper;
import org.woheller69.weather.database.WeekForecast;
import org.woheller69.weather.ui.RecycleList.CityWeatherAdapter;
import org.woheller69.weather.ui.RecycleList.OnSwipeDownListener;
import org.woheller69.weather.ui.updater.IUpdateableCityUI;
import org.woheller69.weather.ui.updater.ViewUpdater;
import org.woheller69.weather.ui.viewPager.WeatherPagerAdapter;

import java.util.List;

public class WeatherCityFragment extends Fragment implements IUpdateableCityUI {

    private int mCityId = -1;
    private int[] mDataSetTypes = new int[]{};
    private static int[] mFull = {OVERVIEW, DAY, WEEK, CHART}; //TODO Make dynamic from Settings
    private static int[] mEmpty = {EMPTY};
    private CityWeatherAdapter mAdapter;

    private RecyclerView recyclerView;

    public static WeatherCityFragment newInstance(Bundle args)
    {
        WeatherCityFragment weatherCityFragment = new WeatherCityFragment();
        weatherCityFragment.setArguments(args);
        return weatherCityFragment;
    }

    public void setAdapter(CityWeatherAdapter adapter) {
        mAdapter = adapter;

        if (recyclerView != null) {
            recyclerView.setAdapter(mAdapter);
            recyclerView.setFocusable(false);
            recyclerView.setLayoutManager(getLayoutManager(getContext()));  //fixes problems with StaggeredGrid: After refreshing data only empty space shown below tab
        }
    }

    public void loadData() {
                GeneralData generalData = SQLiteHelper.getInstance(getContext()).getGeneralDataByCityId(mCityId);
                if (generalData.getTimestamp()==0) mDataSetTypes=mEmpty;  //show empty view if no data available yet
                else mDataSetTypes=mFull;
                mAdapter = new CityWeatherAdapter(generalData, mDataSetTypes, getContext());
                setAdapter(mAdapter);
            }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        ViewUpdater.addSubscriber(this);
    }

    @Override
    public void onDetach() {
        ViewUpdater.removeSubscriber(this);

        super.onDetach();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_weather_forecast_city_overview, container, false);

        recyclerView = v.findViewById(R.id.weatherForecastRecyclerView);
        recyclerView.setLayoutManager(getLayoutManager(getContext()));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(-1)){
                    recyclerView.setOnTouchListener(new OnSwipeDownListener(getContext()) {
                        public void onSwipeDown() {
                                WeatherPagerAdapter.refreshSingleData(getContext(),true,mCityId);
                                ForecastCityActivity.startRefreshAnimation();
                        }
                    });
                }else recyclerView.setOnTouchListener(null);
            }
        });

        Bundle args = getArguments();
        mCityId = args.getInt("city_id");

        loadData();

        return v;
    }

    public RecyclerView.LayoutManager getLayoutManager(Context context) {
            return new LinearLayoutManager(context);
    }

    @Override
    public void processNewGeneralData(GeneralData data) {
        if (data != null && data.getCity_id() == mCityId) {
            mDataSetTypes= mFull;
            setAdapter(new CityWeatherAdapter(data, mDataSetTypes, getContext()));
        }
    }

    @Override
    public void processNewForecasts(List<HourlyForecast> hourlyForecasts, List<WeekForecast> weekForecasts) {
        if (hourlyForecasts != null && hourlyForecasts.size() > 0 && hourlyForecasts.get(0).getCity_id() == mCityId) {
            if (mAdapter != null) {
                mAdapter.updateForecastData(hourlyForecasts, weekForecasts);
            }
        }
    }
}
