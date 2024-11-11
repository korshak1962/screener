package korshak.com.screener.serviceImpl.chart;

import java.awt.geom.Point2D;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.XYPlot;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.axis.ValueAxis;

public class ChartController {
  private static final double SCALE_FACTOR = 0.1; // For mouse wheel scaling
  private static final double ZOOM_FACTOR = 0.5;  // For zoom buttons
  private ChartPanel chartPanel;
  private CrosshairManager crosshairManager;
  private boolean crosshairEnabled = true;

  public void initialize(ChartPanel chartPanel) {
    this.chartPanel = chartPanel;
    this.crosshairManager = new CrosshairManager(chartPanel);
    setupListeners();
  }

  private void setupListeners() {
    crosshairManager.setupCrosshairListeners();

    // Add mouse wheel listener for scaling
    chartPanel.addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        handleAxisScale(e);
      }
    });
  }

  private void handleAxisScale(MouseWheelEvent e) {
    XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
    Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());
    Rectangle2D plotArea = chartPanel.getScreenDataArea();

    if (e.isShiftDown()) {
      // Scale time axis when shift is held
      ValueAxis domainAxis = plot.getDomainAxis();
      double centerX = domainAxis.java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
      scaleAxisAroundValue(domainAxis, e.getWheelRotation(), centerX);
    } else {
      // Scale price axis
      ValueAxis rangeAxis = plot.getRangeAxis();
      double centerY = rangeAxis.java2DToValue(p.getY(), plotArea, plot.getRangeAxisEdge());
      scaleAxisAroundValue(rangeAxis, e.getWheelRotation(), centerY);
    }
  }

  private void scaleAxisAroundValue(ValueAxis axis, int wheelRotation, double centerValue) {
    // Get current bounds
    double lower = axis.getLowerBound();
    double upper = axis.getUpperBound();
    double length = upper - lower;

    // Calculate how much to expand/contract the range
    double scaleFactor = wheelRotation < 0 ? (1 + SCALE_FACTOR) : (1 - SCALE_FACTOR);
    double newLength = length * scaleFactor;

    // Calculate new bounds while keeping the center point
    double ratio = (centerValue - lower) / length;
    double newLower = centerValue - (ratio * newLength);
    double newUpper = newLower + newLength;

    // Set new range
    axis.setRange(newLower, newUpper);
  }

  // Menu button methods
  public void zoomIn() {
    if (chartPanel != null) {
      XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
      ValueAxis domainAxis = plot.getDomainAxis();
      ValueAxis rangeAxis = plot.getRangeAxis();

      // Get the center of each axis
      double domainCenter = (domainAxis.getLowerBound() + domainAxis.getUpperBound()) / 2;
      double rangeCenter = (rangeAxis.getLowerBound() + rangeAxis.getUpperBound()) / 2;

      // Zoom in both axes
      scaleAxisAroundValue(domainAxis, 1, domainCenter);
      scaleAxisAroundValue(rangeAxis, 1, rangeCenter);
    }
  }

  public void zoomOut() {
    if (chartPanel != null) {
      XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
      ValueAxis domainAxis = plot.getDomainAxis();
      ValueAxis rangeAxis = plot.getRangeAxis();

      // Get the center of each axis
      double domainCenter = (domainAxis.getLowerBound() + domainAxis.getUpperBound()) / 2;
      double rangeCenter = (rangeAxis.getLowerBound() + rangeAxis.getUpperBound()) / 2;

      // Zoom out both axes
      scaleAxisAroundValue(domainAxis, -1, domainCenter);
      scaleAxisAroundValue(rangeAxis, -1, rangeCenter);
    }
  }

  public void resetZoom() {
    if (chartPanel != null) {
      XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
      plot.getDomainAxis().setAutoRange(true);
      plot.getRangeAxis().setAutoRange(true);
    }
  }

  public void toggleCrosshair(boolean enabled) {
    this.crosshairEnabled = enabled;
    crosshairManager.setEnabled(enabled);
  }
}