package github.lzr.com.noisedetector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

/**
 * Created by Administrator on 2018/2/2 0002.
 */

public class ReportActivity extends Activity {

    private ScatterChart mScatterChart;
    private TextView totalCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        mScatterChart = (ScatterChart) findViewById(R.id.scatter_chart);
        if (mScatterChart == null) {
            Log.d("lizheren", "onCreate: " + mScatterChart);
        }
        totalCount = (TextView) findViewById(R.id.tv_totalcount);
        getValue();
    }

    public void back(View view) {
        finish();
    }

    private void getValue() {
        Intent intent = getIntent();
        ArrayList<Integer> sizeList = intent.getIntegerArrayListExtra("sizeList");
        ArrayList<Integer> lightScaleList = intent.getIntegerArrayListExtra("lightScaleList");
        totalCount.setText("Total Count=" + sizeList.size());
        setScatterChart(sizeList, lightScaleList);
    }

    private void setScatterChart(ArrayList<Integer> sizeList, ArrayList<Integer> lightScaleList) {
        initScatterChart();
        setData(sizeList, lightScaleList);
    }

    private void setData(ArrayList<Integer> sizeList, ArrayList<Integer> lightScaleList) {
        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        for (int i = 0; i < sizeList.size(); i++) {
            yVals1.add(new Entry(sizeList.get(i), lightScaleList.get(i)));
        }
        //创建一个数据集,并给它一个类型
        ScatterDataSet set1 = new ScatterDataSet(yVals1, "");
        set1.setScatterShape(ScatterChart.ScatterShape.SQUARE);
        //设置颜色
        set1.setColor(ColorTemplate.COLORFUL_COLORS[0]);
        set1.setScatterShapeSize(8f);

        ArrayList<IScatterDataSet> dataSets = new ArrayList<IScatterDataSet>();
        dataSets.add(set1);

        //创建一个数据集的数据对象
        ScatterData data = new ScatterData(dataSets);

        mScatterChart.setData(data);
        mScatterChart.invalidate();
    }

    private void initScatterChart() {
        //散点图
        mScatterChart.getDescription().setEnabled(false);

        mScatterChart.setDrawGridBackground(false);
        mScatterChart.setTouchEnabled(true);
        mScatterChart.setMaxHighlightDistance(10f);

        // 支持缩放和拖动
        mScatterChart.setDragEnabled(true);
        mScatterChart.setScaleEnabled(true);

        mScatterChart.setMaxVisibleValueCount(10);
        mScatterChart.setPinchZoom(true);

        mScatterChart.getLegend().setEnabled(false);
/*        Legend l = mScatterChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setXOffset(5f);*/

        YAxis yl = mScatterChart.getAxisLeft();
        yl.setAxisMinimum(0f);

        mScatterChart.getAxisRight().setEnabled(false);
        XAxis xl = mScatterChart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setDrawGridLines(false);
    }

}
