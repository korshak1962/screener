package korshak.com.screener.serviceImpl.chart;

import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

public class CrosshairManager {
  private final ChartPanel chartPanel;
  private final SimpleDateFormat dateFormat;
  private final CrosshairOverlayPanel overlay;
  private XYPlot currentPlot;
  private double currentX;
  private double currentY;

  public CrosshairManager(ChartPanel chartPanel) {
    this.chartPanel = chartPanel;
    this.dateFormat = new SimpleDateFormat("dd MMM yyyy");
    this.overlay = new CrosshairOverlayPanel(chartPanel);

    // Find the root pane container and set up the glass pane
    Container parent = chartPanel;
    while (parent != null && !(parent instanceof RootPaneContainer)) {
      parent = parent.getParent();
    }

    if (parent instanceof RootPaneContainer root) {
      root.setGlassPane(overlay);
      overlay.setVisible(true);

      // Add mouse motion listener to the chart panel
      chartPanel.addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
          updateCrosshair(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
          updateCrosshair(e);
        }
      });
    }
  }

  private void updateCrosshair(MouseEvent e) {
    Rectangle2D dataArea = chartPanel.getScreenDataArea();
    if (!dataArea.contains(e.getPoint())) {
      overlay.updateLabels(null, "", "");
      return;
    }

    // Convert the point to screen coordinates
    Point screenPoint = e.getPoint();
    SwingUtilities.convertPointToScreen(screenPoint, chartPanel);

    XYPlot targetPlot;

    if (chartPanel.getChart().getPlot() instanceof CombinedDomainXYPlot combinedPlot) {
      @SuppressWarnings("unchecked")
      List<XYPlot> subplots = combinedPlot.getSubplots();

      double relativeY = e.getY() - dataArea.getY();
      double totalHeight = dataArea.getHeight();
      double mainPlotHeight = totalHeight * 0.75;
      double gap = combinedPlot.getGap();

      if (relativeY <= mainPlotHeight) {
        targetPlot = subplots.get(0);
        Rectangle2D mainArea = new Rectangle2D.Double(
            dataArea.getX(), dataArea.getY(),
            dataArea.getWidth(), mainPlotHeight
        );
        updateValues(targetPlot, e.getPoint(), mainArea, screenPoint);
      } else {
        targetPlot = subplots.get(1);
        Rectangle2D histArea = new Rectangle2D.Double(
            dataArea.getX(),
            dataArea.getY() + mainPlotHeight + gap,
            dataArea.getWidth(),
            totalHeight - mainPlotHeight - gap
        );
        updateValues(targetPlot, e.getPoint(), histArea, screenPoint);
      }
    } else {
      targetPlot = (XYPlot) chartPanel.getChart().getPlot();
      updateValues(targetPlot, e.getPoint(), dataArea, screenPoint);
    }
  }

  private void updateValues(XYPlot plot, Point point, Rectangle2D dataArea, Point screenPoint) {
    currentPlot = plot;

    currentX = plot.getDomainAxis().java2DToValue(point.getX(), dataArea, plot.getDomainAxisEdge());
    String dateLabel = dateFormat.format(new Date((long) currentX));

    currentY = plot.getRangeAxis().java2DToValue(point.getY(), dataArea, plot.getRangeAxisEdge());

    StringBuilder label = new StringBuilder();

    if (plot.getDataset(0) instanceof OHLCDataset ohlcDataset) {
      int item = findClosestItem(ohlcDataset, (long) currentX);
      if (item >= 0) {
        double open = ohlcDataset.getOpenValue(0, item);
        double high = ohlcDataset.getHighValue(0, item);
        double low = ohlcDataset.getLowValue(0, item);
        double close = ohlcDataset.getCloseValue(0, item);
        label.append(String.format("O:%.2f H:%.2f L:%.2f C:%.2f", open, high, low, close));
      }

      XYDataset smaDataset = plot.getDataset(2);
      if (smaDataset != null) {
        for (int series = 0; series < smaDataset.getSeriesCount(); series++) {
          int smaItem = findClosestItem(smaDataset, series, (long) currentX);
          if (smaItem >= 0) {
            double smaValue = smaDataset.getYValue(series, smaItem);
            String smaName = smaDataset.getSeriesKey(series).toString();
            label.append(String.format(" | %s:%.2f", smaName, smaValue));
          }
        }
      }
    } else if (plot.getDataset() instanceof TradeHistogramDataset histDataset) {
      int item = findClosestTradeItem(histDataset, (long) currentX);
      if (item >= 0) {
        double pnl = histDataset.getY(0, item).doubleValue();
        label.append(String.format("PnL: %.2f", pnl));
      }
    }

    overlay.updateLabels(screenPoint, dateLabel, label.toString());
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

  private int findClosestTradeItem(TradeHistogramDataset dataset, long targetX) {
    double minDistance = Double.MAX_VALUE;
    int closestItem = -1;

    for (int i = 0; i < dataset.getItemCount(0); i++) {
      long startX = dataset.getStartX(i);
      long endX = dataset.getEndX(i);
      long midX = (startX + endX) / 2;
      double distance = Math.abs(midX - targetX);
      if (distance < minDistance) {
        minDistance = distance;
        closestItem = i;
      }
    }

    return closestItem;
  }

  public void setEnabled(boolean enabled) {
    overlay.setEnabled(enabled);
  }
}