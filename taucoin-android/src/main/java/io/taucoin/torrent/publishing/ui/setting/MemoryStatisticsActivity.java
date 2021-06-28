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
import io.taucoin.torrent.publishing.core.model.data.MemoryStatistics;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.StatisticRepository;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.LargeValueFormatter;
import io.taucoin.torrent.publishing.databinding.ActivityDataStatisticsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.MyMarkerView;

/**
 * 内存统计页面页面
 */
public class MemoryStatisticsActivity extends BaseActivity {

    private static final Logger logger = LoggerFactory.getLogger("MemoryStatisticsActivity");
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
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_memory_statistics);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.lineChart.setRotation(90);
        binding.lineChart.post(() -> {
            int mScreenWidth = binding.lineChart.getWidth();
            int mScreenHeight = binding.lineChart.getHeight();
            int paddingSize = DimensionsUtil.dp2px(MemoryStatisticsActivity.this, 15);
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
        Disposable subscribe = repository.getMemoryStatistics()
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.lineChart.clear();
    }

    private void initLineChart(List<MemoryStatistics> statistics) {
        binding.lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {

            }

            @Override
            public void onNothingSelected() {

            }
        });
        // 设置描述文本不显示
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
        List<Entry> yValues = new ArrayList<>();

        //自定义的MarkerView对象
        MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);
        mv.setChartView(binding.lineChart);
        mv.setXValues(xValues);
        binding.lineChart.setMarker(mv);

        // 统计数据较少时，补充数据
        long initSupplySize = 7;
        if (statistics.size() > initSupplySize) {
            initSupplySize = 2;
        }
        MemoryStatistics statistic = statistics.get(0);
        for (long j = initSupplySize; j > 0; j--) {
            long timestamp = statistic.getTimestamp() - j * 10 * 60;
            xValues.add(DateUtil.formatTime(timestamp, DateUtil.pattern0));
            yValues.add(new Entry(yValues.size(), 0));
        }

        for (int i = 0; i < statistics.size(); i++) {
            statistic = statistics.get(i);
            logger.debug("statistics.timestamp::{}", DateUtil.formatTime(statistic.getTimestamp(), DateUtil.pattern0));
            if (i > 0) {
                MemoryStatistics lastStatistic = statistics.get(i - 1);
                long supplySize = statistic.getTimeKey() - lastStatistic.getTimeKey();
                if (supplySize > 1) {
                    for (long j = 1; j < supplySize; j++) {
                        long timestamp = lastStatistic.getTimestamp() + j * 10 * 60;
                        xValues.add(DateUtil.formatTime(timestamp, DateUtil.pattern0));
                        yValues.add(new Entry(yValues.size(), 0));
                    }
                }
            }
            xValues.add(DateUtil.formatTime(statistic.getTimestamp(), DateUtil.pattern0));
            yValues.add(new Entry(yValues.size(), statistic.getMemoryAvg()));
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
        LineDataSet setComp = new LineDataSet(yValues, getString(R.string.setting_memory_statistics));
        setComp.setAxisDependency(YAxis.AxisDependency.LEFT);
        setComp.setColor(getResources().getColor(R.color.colorPrimary));
        setComp.setDrawFilled(true);
        setComp.setDrawValues(false);
        setComp.setDrawCircles(false);
        setComp.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        setComp.setLineWidth(1f);// 设置线宽

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setComp);

        LineData lineData = new LineData(dataSets);

        binding.lineChart.setData(lineData);
        binding.lineChart.invalidate();

        //设置XY轴动画
        binding.lineChart.animateY(500);
    }
}