package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.widget.LinearLayout;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.DataStatistics;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.StatisticRepository;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.LargeValueFormatter;
import io.taucoin.torrent.publishing.databinding.ActivityDataStatisticsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.Constants;
import io.taucoin.torrent.publishing.ui.customviews.MyMarkerView;

/**
 * 流量数据统计页面页面
 */
public class DataStatisticsActivity extends BaseActivity {

    private static final Logger logger = LoggerFactory.getLogger("DataStatisticsActivity");
    private ActivityDataStatisticsBinding binding;
    private CompositeDisposable disposables = new CompositeDisposable();
    private StatisticRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityUtil.setRequestedOrientation(this);
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_data_statistics);
        repository = RepositoryHelper.getStatisticRepository(getApplicationContext());
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_data_statistics);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.lineChart.setRotation(90);
        binding.lineChart.post(() -> {
            int mScreenWidth = binding.lineChart.getWidth();
            int mScreenHeight = binding.lineChart.getHeight();
            int paddingSize = DimensionsUtil.dp2px(DataStatisticsActivity.this, 15);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) binding.lineChart.getLayoutParams();
            layoutParams.width = mScreenHeight - paddingSize;
            layoutParams.height = mScreenWidth - paddingSize;
            binding.lineChart.setLayoutParams(layoutParams);

            float offsetY = mScreenHeight - mScreenWidth;
            binding.lineChart.setTranslationY((offsetY + paddingSize) * 1f / 2);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Disposable subscribe = repository.getDataStatistics()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(statistics -> {
                    if (statistics != null) {
                        logger.debug("statistics.Size::{}", statistics.size());
                        initLineChart(statistics);
                    }
                }, it -> {
                    logger.error("timestamp::{}, dataSize::{}, memorySize::{}",
                            "error", "error", "error", it);
                });
        disposables.add(subscribe);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    private void initLineChart(List<DataStatistics> statistics) {
        binding.lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {

            }

            @Override
            public void onNothingSelected() {

            }
        });
        // //设置描述文本不显示
        binding.lineChart.getDescription().setEnabled(false);
        //设置是否可以触摸
        binding.lineChart.setTouchEnabled(true);
        binding.lineChart.setDragDecelerationFrictionCoef(0.9f);
        //设置是否可以拖拽
        binding.lineChart.setDragEnabled(true);
        //设置是否可以缩放
        binding.lineChart.setScaleEnabled(true);
        binding.lineChart.setDrawGridBackground(false);
        binding.lineChart.setHighlightPerDragEnabled(true);
        binding.lineChart.setPinchZoom(true);

        // 组织数据
        List<String> xValues = new ArrayList<>();
        List<Entry> yMeteredValues = new ArrayList<>();
        List<Entry> yUnMeteredValues = new ArrayList<>();

        // 统计数据较少时，补充数据
        long initSupplySize = 7;
        if (statistics.size() > initSupplySize) {
            initSupplySize = 2;
        }
        DataStatistics statistic;
        long firstTimestamp;
        if (statistics.size() > 0) {
            statistic = statistics.get(0);
            firstTimestamp = statistic.getTimestamp();
        } else {
            firstTimestamp = DateUtil.getTime();
        }
        for (long j = initSupplySize; j > 0; j--) {
            long timestamp = firstTimestamp - j * Constants.STATISTICS_DISPLAY_PERIOD;
            xValues.add(DateUtil.formatTime(timestamp, DateUtil.pattern0));
            yMeteredValues.add(new Entry(yMeteredValues.size(), 0));
            yUnMeteredValues.add(new Entry(yUnMeteredValues.size(), 0));
        }
        for (int i = 0; i < statistics.size(); i++) {
            statistic = statistics.get(i);
            logger.debug("statistics.timestamp::{}", DateUtil.formatTime(statistic.getTimestamp(), DateUtil.pattern0));
            if (i > 0) {
                DataStatistics lastStatistic = statistics.get(i - 1);
                long supplySize = statistic.getTimeKey() - lastStatistic.getTimeKey();
                if (supplySize > 1) {
                    for (long j = 1; j < supplySize; j++) {
                        long timestamp = lastStatistic.getTimestamp() + j * Constants.STATISTICS_DISPLAY_PERIOD;
                        xValues.add(DateUtil.formatTime(timestamp, DateUtil.pattern0));
                        yMeteredValues.add(new Entry(yMeteredValues.size(), 0));
                        yUnMeteredValues.add(new Entry(yUnMeteredValues.size(), 0));
                    }
                }
            }
            xValues.add(DateUtil.formatTime(statistic.getTimestamp(), DateUtil.pattern0));
            yMeteredValues.add(new Entry(yMeteredValues.size(), statistic.getMeteredDataAvg()));
            yUnMeteredValues.add(new Entry(yUnMeteredValues.size(), statistic.getUnMeteredDataAvg()));
        }

        // X轴
        String[] values = new String[xValues.size()];
        xValues.toArray(values);
        ValueFormatter xAxisFormatter = new IndexAxisValueFormatter(values);
        XAxis xAxis = binding.lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(0f);
        xAxis.setValueFormatter(xAxisFormatter);
        xAxis.setDrawAxisLine(true);//是否绘制轴线
        xAxis.setAvoidFirstLastClipping(true);

        //设置一页最大显示个数为6，超出部分就滑动
//        float ratio = (float) values.length / (float) 6;
//        //显示的时候是按照多大的比率缩放显示,1f表示不放大缩小
//        binding.lineChart.zoom(ratio,1f,0,0);

        // Y轴
        LargeValueFormatter yAxisFormatter = new LargeValueFormatter();
        YAxis leftAxis = binding.lineChart.getAxisLeft();

        leftAxis.setLabelCount(8, false);
        leftAxis.setValueFormatter(yAxisFormatter);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);
        leftAxis.setAxisMinimum(0);
        binding.lineChart.getAxisRight().setEnabled(false);

        //这里，每重新new一个LineDataSet，相当于重新画一组折线
        //每一个LineDataSet相当于一组折线。比如:这里有两个LineDataSet：setComp1，setComp2。
        LineDataSet meteredDataSet = new LineDataSet(yMeteredValues, getString(R.string.setting_metered_statistics));
        meteredDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        meteredDataSet.setColor(getResources().getColor(R.color.color_red));
        meteredDataSet.setDrawFilled(true);
        meteredDataSet.setDrawValues(false);
        meteredDataSet.setDrawCircles(false);
        meteredDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        meteredDataSet.setLineWidth(1f);// 设置线宽

        LineDataSet unMeteredDataSet = new LineDataSet(yUnMeteredValues, getString(R.string.setting_wifi_statistics));
        unMeteredDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        unMeteredDataSet.setColor(getResources().getColor(R.color.colorPrimary));
        unMeteredDataSet.setDrawFilled(true);
        unMeteredDataSet.setDrawValues(false);
        unMeteredDataSet.setDrawCircles(false);
        unMeteredDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        unMeteredDataSet.setLineWidth(1f);// 设置线宽

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(meteredDataSet);
        dataSets.add(unMeteredDataSet);

        LineData lineData = new LineData(dataSets);

        //自定义的MarkerView对象
        MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);
        mv.setChartView(binding.lineChart);
        mv.setValueFormatter(yAxisFormatter);
        mv.setXValues(xValues);
        binding.lineChart.setMarker(mv);

        binding.lineChart.setData(lineData);
        binding.lineChart.invalidate();

        //设置XY轴动画
        binding.lineChart.animateY(500);
    }
}