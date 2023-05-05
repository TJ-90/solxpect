package org.woheller69.weather.ui.RecycleList;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.woheller69.weather.R;

/**
 * This class holds instances of items that are to be displayed in the list.
 * The idea of this class has been taken from
 * https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf#.hmhbe8sku
 * as of 2016-08-03. Parts of the code were copied from that source.
 */
public class ItemViewHolder extends RecyclerView.ViewHolder {

    /**
     * Member variables
     */
    public TextView cityName;
    public TextView tiltAngle;
    public TextView azimuthAngle;
    public TextView cellsMaxPower;
    public TextView cellsArea;
    public TextView cellsEfficiency;
    public TextView inverterPowerLimit;
    public TextView inverterEfficiency;


    /**
     * Constructor.
     *
     * @param itemView The view that contains the fields that are to be set for each list item.
     */
    public ItemViewHolder(View itemView) {
        super(itemView);
        this.cityName = (TextView) itemView.findViewById(R.id.city_overview_list_item_text);
        this.tiltAngle = (TextView) itemView.findViewById(R.id.city_tilt_angle);
        this.azimuthAngle = (TextView)  itemView.findViewById(R.id.city_azimuth_angle);
        this.cellsMaxPower = (TextView) itemView.findViewById(R.id.city_cells_max_power);
        this.cellsArea = (TextView) itemView.findViewById(R.id.city_cells_area);
        this.cellsEfficiency = (TextView) itemView.findViewById(R.id.city_cells_efficiency);
        this.inverterPowerLimit = (TextView) itemView.findViewById(R.id.city_inverter_power_limit);
        this.inverterEfficiency = (TextView) itemView.findViewById(R.id.city_inverter_efficiency);

    }

}