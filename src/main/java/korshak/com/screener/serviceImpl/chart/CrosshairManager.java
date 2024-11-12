package korshak.com.screener.serviceImpl.chart;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.chart.ui.RectangleEdge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CrosshairManager {
  private final ChartPanel chartPanel;
  private final SimpleDateFormat dateFormat;
  private boolean enabled = true;
  private Point currentPoint;
  private String priceLabel = "";
  private String dateLabel = "";
  private XYPlot currentPlot;

  public CrosshairManager(ChartPanel chartPanel) {
    this.chartPanel = chartPanel;
    this.dateFormat = new SimpleDateFormat("dd MMM yyyy");
    setupOverlay();
  }

  private void setupOverlay() {
    JPanel overlay = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (enabled && currentPoint != null) {
          paintCrosshair((Graphics2D) g);
        }
      }
    };
    overlay.setOpaque(false);
    chartPanel.add(overlay);
    overlay.setBounds(0, 0, chartPanel.getWidth(), chartPanel.getHeight());
  }

  private void paintCrosshair(Graphics2D g2) {
    Rectangle2D dataArea = chartPanel.getScreenDataArea();
    if (!dataArea.contains(currentPoint)) {
      return;
    }

    // Set up graphics
    g2.setColor(Color.GRAY);
    g2.setStroke(new BasicStroke(1.0f));

    // Draw vertical line
    g2.drawLine(currentPoint.x, (int)dataArea.getMinY(),
        currentPoint.x, (int)dataArea.getMaxY());

    // Draw horizontal line only in current plot area
    if (currentPlot != null) {
      double relativeY = currentPoint.getY() - dataArea.getY();
      double totalHeight = dataArea.getHeight();
      double mainPlotHeight = totalHeight * 0.75;

      if (relativeY <= mainPlotHeight) {
        // Main plot horizontal line
        g2.drawLine((int)dataArea.getMinX(), currentPoint.y,
            (int)dataArea.getMaxX(), currentPoint.y);
      } else {
        // Histogram plot horizontal line
        int y = currentPoint.y;
        g2.drawLine((int)dataArea.getMinX(), y,
            (int)dataArea.getMaxX(), y);
      }
    }

    // Draw labels
    drawLabels(g2, dataArea);
  }

  private void drawLabels(Graphics2D g2, Rectangle2D dataArea) {
    g2.setFont(new Font("SansSerif", Font.PLAIN, 11));

    // Background for labels
    Color bgColor = new Color(255, 255, 255, 200);
    g2.setColor(bgColor);

    // Date label (at bottom)
    FontMetrics fm = g2.getFontMetrics();
    int dateWidth = fm.stringWidth(dateLabel);
    int dateHeight = fm.getHeight();
    int dateX = currentPoint.x - dateWidth / 2;
    int dateY = (int)(dataArea.getMaxY() + dateHeight);

    g2.fillRect(dateX - 2, dateY - dateHeight, dateWidth + 4, dateHeight);
    g2.setColor(Color.BLACK);
    g2.drawString(dateLabel, dateX, dateY - 2);

    // Price/Value label (on right)
    int priceWidth = fm.stringWidth(priceLabel);
    int priceX = (int)dataArea.getMaxX() - priceWidth - 4;
    int priceY = currentPoint.y;

    g2.setColor(bgColor);
    g2.fillRect(priceX - 2, priceY - dateHeight + 2, priceWidth + 4, dateHeight);
    g2.setColor(Color.BLACK);
    g2.drawString(priceLabel, priceX, priceY + fm.getAscent() - dateHeight + 2);
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
    if (!enabled) {
      return;
    }

    Rectangle2D dataArea = chartPanel.getScreenDataArea();
    if (!dataArea.contains(e.getPoint())) {
      return;
    }

    currentPoint = e.getPoint();
    XYPlot plot;

    if (chartPanel.getChart().getPlot() instanceof CombinedDomainXYPlot) {
      CombinedDomainXYPlot combinedPlot = (CombinedDomainXYPlot) chartPanel.getChart().getPlot();
      @SuppressWarnings("unchecked")
      List<XYPlot> subplots = combinedPlot.getSubplots();

      // Determine which subplot we're in
      double relativeY = e.getY() - dataArea.getY();
      double totalHeight = dataArea.getHeight();
      double mainPlotHeight = totalHeight * 0.75;

      if (relativeY <= mainPlotHeight) {
        plot = subplots.get(0);  // Main plot
      } else {
        plot = subplots.get(1);  // Histogram plot
      }
    } else {
      plot = (XYPlot) chartPanel.getChart().getPlot();
    }

    currentPlot = plot;
    updateLabels(plot, e.getPoint(), dataArea);
    chartPanel.repaint();
  }

  private void updateLabels(XYPlot plot, Point point, Rectangle2D dataArea) {
    // Get x value (date)
    double x = plot.getDomainAxis().java2DToValue(point.getX(), dataArea, plot.getDomainAxisEdge());
    dateLabel = dateFormat.format(new Date((long) x));

    // Get y value and create appropriate label
    double y = plot.getRangeAxis().java2DToValue(point.getY(), dataArea, plot.getRangeAxisEdge());

    StringBuilder label = new StringBuilder();

    if (plot.getDataset(0) instanceof OHLCDataset) {
      OHLCDataset ohlcDataset = (OHLCDataset) plot.getDataset(0);
      int item = findClosestItem(ohlcDataset, (long) x);
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
          int smaItem = findClosestItem(smaDataset, series, (long) x);
          if (smaItem >= 0) {
            double smaValue = smaDataset.getYValue(series, smaItem);
            String smaName = smaDataset.getSeriesKey(series).toString();
            label.append(String.format(" | %s:%.2f", smaName, smaValue));
          }
        }
      }
    } else if (plot.getDataset() instanceof TradeHistogramDataset) {
      TradeHistogramDataset histDataset = (TradeHistogramDataset) plot.getDataset();
      int item = findClosestTradeItem(histDataset, (long) x);
      if (item >= 0) {
        double pnl = histDataset.getY(0, item).doubleValue();
        label.append(String.format("PnL: %.2f", pnl));
      }
    }

    priceLabel = label.toString();
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
    this.enabled = enabled;
    if (!enabled) {
      currentPoint = null;
      chartPanel.repaint();
    }
  }
}