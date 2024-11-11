package korshak.com.screener.serviceImpl.chart;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.labels.CrosshairLabelGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.OHLCDataset;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrosshairManager {
  private final ChartPanel chartPanel;
  private final Crosshair xCrosshair;
  private final Crosshair yCrosshair;
  private boolean enabled = true;
  private final SimpleDateFormat dateFormat;

  public CrosshairManager(ChartPanel chartPanel) {
    this.chartPanel = chartPanel;
    this.dateFormat = new SimpleDateFormat("dd MMM yyyy");

    this.xCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(1.0f));
    this.yCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(1.0f));

    // Custom label generator for date (x-axis)
    xCrosshair.setLabelGenerator(new CrosshairLabelGenerator() {
      @Override
      public String generateLabel(Crosshair crosshair) {
        return dateFormat.format(new Date((long) crosshair.getValue()));
      }
    });

    // Custom label generator for y-axis showing price and SMA values
    yCrosshair.setLabelGenerator(new CrosshairLabelGenerator() {
      @Override
      public String generateLabel(Crosshair crosshair) {
        XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
        StringBuilder label = new StringBuilder();

        // Get current price from OHLC dataset
        OHLCDataset ohlcDataset = (OHLCDataset) plot.getDataset(0);
        if (ohlcDataset != null) {
          int item = findClosestItem(ohlcDataset, (long) xCrosshair.getValue());
          if (item >= 0) {
            double open = ohlcDataset.getOpenValue(0, item);
            double high = ohlcDataset.getHighValue(0, item);
            double low = ohlcDataset.getLowValue(0, item);
            double close = ohlcDataset.getCloseValue(0, item);
            label.append(String.format("O:%.2f H:%.2f L:%.2f C:%.2f", open, high, low, close));
          }
        }

        // Get SMA values
        XYDataset smaDataset = plot.getDataset(2);
        if (smaDataset != null) {
          for (int series = 0; series < smaDataset.getSeriesCount(); series++) {
            int item = findClosestItem(smaDataset, series, (long) xCrosshair.getValue());
            if (item >= 0) {
              double smaValue = smaDataset.getYValue(series, item);
              String smaName = smaDataset.getSeriesKey(series).toString();
              label.append(String.format(" | %s:%.2f", smaName, smaValue));
            }
          }
        }

        return label.toString();
      }
    });

    initializeCrosshairs();
  }

  private void initializeCrosshairs() {
    xCrosshair.setLabelVisible(true);
    yCrosshair.setLabelVisible(true);

    // Customize label appearance
    xCrosshair.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
    yCrosshair.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
    xCrosshair.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));
    yCrosshair.setLabelFont(new Font("SansSerif", Font.PLAIN, 11));

    CrosshairOverlay overlay = new CrosshairOverlay();
    overlay.addDomainCrosshair(xCrosshair);
    overlay.addRangeCrosshair(yCrosshair);
    chartPanel.addOverlay(overlay);
  }

  public void setupCrosshairListeners() {
    chartPanel.addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseDragged(MouseEvent e) {
        updateCrosshair(e);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        updateCrosshair(e);
      }
    });
  }

  private void updateCrosshair(MouseEvent e) {
    if (!enabled) return;

    Rectangle2D dataArea = chartPanel.getScreenDataArea();
    if (dataArea.contains(e.getPoint())) {
      XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
      double x = plot.getDomainAxis().java2DToValue(
          e.getX(), dataArea, plot.getDomainAxisEdge());
      double y = plot.getRangeAxis().java2DToValue(
          e.getY(), dataArea, plot.getRangeAxisEdge());
      xCrosshair.setValue(x);
      yCrosshair.setValue(y);
    }
  }

  private int findClosestItem(XYDataset dataset, int series, long targetX) {
    double minDistance = Double.MAX_VALUE;
    int closestItem = -1;

    for (int i = 0; i < dataset.getItemCount(series); i++) {
      double itemX = dataset.getXValue(series, i);
      double distance = Math.abs(itemX - targetX);
      if (distance < minDistance) {
        minDistance = distance;
        closestItem = i;
      }
    }

    return closestItem;
  }

  private int findClosestItem(OHLCDataset dataset, long targetX) {
    double minDistance = Double.MAX_VALUE;
    int closestItem = -1;

    for (int i = 0; i < dataset.getItemCount(0); i++) {
      double itemX = dataset.getXValue(0, i);
      double distance = Math.abs(itemX - targetX);
      if (distance < minDistance) {
        minDistance = distance;
        closestItem = i;
      }
    }

    return closestItem;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      xCrosshair.setValue(Double.NaN);
      yCrosshair.setValue(Double.NaN);
    }
  }
}