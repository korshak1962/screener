package korshak.com.screener.serviceImpl.chart;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

public class ChartClickListener implements ChartMouseListener {
  private static final double CLICK_TOLERANCE = 20.0; // Pixels tolerance for clicking near a line
  private final SimpleDateFormat dateFormat;

  public ChartClickListener() {
    this.dateFormat = new SimpleDateFormat("dd MMM yyyy");
  }

  @Override
  public void chartMouseClicked(ChartMouseEvent event) {
    ChartPanel chartPanel = (ChartPanel) event.getTrigger().getSource();
    XYPlot plot = (XYPlot) event.getChart().getPlot();
    Point2D p = chartPanel.translateScreenToJava2D(event.getTrigger().getPoint());
    Rectangle2D plotArea = chartPanel.getScreenDataArea();

    double chartX =
        plot.getDomainAxis().java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
    double chartY = plot.getRangeAxis().java2DToValue(p.getY(), plotArea, plot.getRangeAxisEdge());

    // First try to find SMA values near click point
    XYDataset smaDataset = plot.getDataset(2);
    if (smaDataset != null) {
      if (handleSmaClick(smaDataset, chartX, chartY, p, plotArea, plot)) {
        return; // If we found and displayed SMA info, we're done
      }
    }

    // If no SMA was clicked, try candlestick
    OHLCDataset candlestickData = (OHLCDataset) plot.getDataset(0);
    if (candlestickData != null) {
      int item = findClosestItem(candlestickData, (long) chartX);
      if (item >= 0) {
        displayCandlestickInfo(candlestickData, item);
      }
    }
  }

  @Override
  public void chartMouseMoved(ChartMouseEvent event) {
    // Not needed for click handling
  }

  private boolean handleSmaClick(XYDataset dataset, double chartX, double chartY,
                                 Point2D mousePoint, Rectangle2D plotArea, XYPlot plot) {
    double minDistance = Double.MAX_VALUE;
    String closestSma = null;
    double closestValue = 0;
    long closestTime = 0;

    for (int series = 0; series < dataset.getSeriesCount(); series++) {
      int item = findClosestItem(dataset, series, (long) chartX);
      if (item >= 0) {
        double xValue = dataset.getXValue(series, item);
        double yValue = dataset.getYValue(series, item);

        // Convert data points to screen coordinates
        double screenX =
            plot.getDomainAxis().valueToJava2D(xValue, plotArea, plot.getDomainAxisEdge());
        double screenY =
            plot.getRangeAxis().valueToJava2D(yValue, plotArea, plot.getRangeAxisEdge());

        // Calculate distance using point-to-point distance formula
        double dx = mousePoint.getX() - screenX;
        double dy = mousePoint.getY() - screenY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < minDistance && distance < CLICK_TOLERANCE) {
          minDistance = distance;
          closestSma = dataset.getSeriesKey(series).toString();
          closestValue = yValue;
          closestTime = (long) xValue;
        }
      }
    }
    return false;
  }

  private void displayCandlestickInfo(OHLCDataset dataset, int item) {
    Date date = new Date((long) dataset.getXValue(0, item));
    double open = dataset.getOpenValue(0, item);
    double high = dataset.getHighValue(0, item);
    double low = dataset.getLowValue(0, item);
    double close = dataset.getCloseValue(0, item);
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
}