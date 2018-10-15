package com.github.mikephil.charting.formatter;

import com.github.mikephil.charting.components.AxisBase;

import java.util.List;

/**
 * Created by ww on 2017/5/17.
 * 折线图设置X轴的数据u
 */

public class CustomXValueFormatter implements IAxisValueFormatter {

    private List<String> xLables;

    public CustomXValueFormatter(List<String> xLables){

        this.xLables = xLables;

    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {

        if(xLables.size() == 0){
            return "";
        }

        return xLables.get((int) value % xLables.size());
    }

}
