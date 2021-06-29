package io.taucoin.torrent.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;

import org.slf4j.LoggerFactory;

import java.util.List;

import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.LargeValueFormatter;

/**
 * 自定义MyMarkerView
 */
@SuppressLint("ViewConstructor")
public class MyMarkerView extends MarkerView {

    private TextView tvContent;
    private LargeValueFormatter formatter;
    private List<String> xValues;

    public MyMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e instanceof CandleEntry) {
            CandleEntry ce = (CandleEntry) e;
            tvContent.setText(Utils.formatNumber(ce.getHigh(), 2, false));
        } else {
            String value = getValueFormatter().getFormattedValue(e.getY());
            if (xValues != null && e.getX() < xValues.size()) {
                value =xValues.get((int) e.getX()) + ", " + value;
            }
            tvContent.setText(value);
//            tvContent.setText(Utils.formatNumber(e.getY(), 2, false));
        }
        LoggerFactory.getLogger("MyMarkerView").debug("MyMarkerView text::{}",
                tvContent.getText().toString());
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() * 1.0f / 2), -getHeight());
    }

    public void setXValues(List<String> xValues) {
        this.xValues = xValues;
    }

    private LargeValueFormatter getValueFormatter() {
        if (null == formatter) {
            formatter = new LargeValueFormatter();
        }
        return formatter;
    }

    public void setValueFormatter(LargeValueFormatter formatter) {
        this.formatter = formatter;
    }
}
