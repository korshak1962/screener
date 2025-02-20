package korshak.com.screener.serviceImpl.chart;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;

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
    //crosshairManager.setupCrosshairListeners();

    // Add mouse wheel listener for scaling
    chartPanel.addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        handleAxisScale(e);
      }
    });
  }

  private void handleAxisScale(MouseWheelEvent e) {
    Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());
    Rectangle2D plotArea = chartPanel.getScreenDataArea();

    if (chartPanel.getChart().getPlot() instanceof CombinedDomainXYPlot combinedPlot) {

      // Handle time axis scaling when shift is held
      if (e.isShiftDown()) {
        ValueAxis domainAxis = combinedPlot.getDomainAxis();
        if (domainAxis != null) {
          double centerX =
              domainAxis.java2DToValue(p.getX(), plotArea, combinedPlot.getDomainAxisEdge());
          scaleAxisAroundValue(domainAxis, e.getWheelRotation(), centerX);
        }
        return;
      }

      // Determine which subplot to scale based on mouse position
      @SuppressWarnings("unchecked")
      List<XYPlot> subplots = combinedPlot.getSubplots();

      double relativeY = e.getY() - plotArea.getY();
      double totalHeight = plotArea.getHeight();
      double mainPlotHeight = totalHeight * 0.75;

      XYPlot targetPlot;
      Rectangle2D targetArea;

      if (relativeY <= mainPlotHeight) {
        // Main plot
        targetPlot = subplots.get(0);
        targetArea = new Rectangle2D.Double(
            plotArea.getX(),
            plotArea.getY(),
            plotArea.getWidth(),
            mainPlotHeight
        );
      } else {
        // Histogram plot
        targetPlot = subplots.get(1);
        targetArea = new Rectangle2D.Double(
            plotArea.getX(),
            plotArea.getY() + mainPlotHeight + combinedPlot.getGap(),
            plotArea.getWidth(),
            totalHeight - mainPlotHeight - combinedPlot.getGap()
        );
      }

      ValueAxis rangeAxis = targetPlot.getRangeAxis();
      if (rangeAxis != null) {
        double centerY =
            rangeAxis.java2DToValue(p.getY(), targetArea, targetPlot.getRangeAxisEdge());
        scaleAxisAroundValue(rangeAxis, e.getWheelRotation(), centerY);
      }
    } else {
      XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
      if (e.isShiftDown()) {
        ValueAxis domainAxis = plot.getDomainAxis();
        if (domainAxis != null) {
          double centerX = domainAxis.java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
          scaleAxisAroundValue(domainAxis, e.getWheelRotation(), centerX);
        }
      } else {
        ValueAxis rangeAxis = plot.getRangeAxis();
        if (rangeAxis != null) {
          double centerY = rangeAxis.java2DToValue(p.getY(), plotArea, plot.getRangeAxisEdge());
          scaleAxisAroundValue(rangeAxis, e.getWheelRotation(), centerY);
        }
      }
    }
  }

  private void scaleAxisAroundValue(ValueAxis axis, int wheelRotation, double centerValue) {
    double lower = axis.getLowerBound();
    double upper = axis.getUpperBound();
    double length = upper - lower;

    double scaleFactor = wheelRotation < 0 ? (1 + SCALE_FACTOR) : (1 - SCALE_FACTOR);
    double newLength = length * scaleFactor;

    double ratio = (centerValue - lower) / length;
    double newLower = centerValue - (ratio * newLength);
    double newUpper = newLower + newLength;

    axis.setRange(newLower, newUpper);
  }

  public void zoomIn() {
    if (chartPanel != null) {
      if (chartPanel.getChart().getPlot() instanceof CombinedDomainXYPlot combinedPlot) {
        ValueAxis domainAxis = combinedPlot.getDomainAxis();
        double domainCenter = (domainAxis.getLowerBound() + domainAxis.getUpperBound()) / 2;
        scaleAxisAroundValue(domainAxis, 1, domainCenter);

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = combinedPlot.getSubplots();
        for (XYPlot subplot : subplots) {
          ValueAxis rangeAxis = subplot.getRangeAxis();
          if (rangeAxis != null) {
            double rangeCenter = (rangeAxis.getLowerBound() + rangeAxis.getUpperBound()) / 2;
            scaleAxisAroundValue(rangeAxis, 1, rangeCenter);
          }
        }
      } else {
        XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        ValueAxis rangeAxis = plot.getRangeAxis();

        double domainCenter = (domainAxis.getLowerBound() + domainAxis.getUpperBound()) / 2;
        double rangeCenter = (rangeAxis.getLowerBound() + rangeAxis.getUpperBound()) / 2;

        scaleAxisAroundValue(domainAxis, 1, domainCenter);
        scaleAxisAroundValue(rangeAxis, 1, rangeCenter);
      }
    }
  }

  public void zoomOut() {
    if (chartPanel != null) {
      if (chartPanel.getChart().getPlot() instanceof CombinedDomainXYPlot combinedPlot) {
        ValueAxis domainAxis = combinedPlot.getDomainAxis();
        double domainCenter = (domainAxis.getLowerBound() + domainAxis.getUpperBound()) / 2;
        scaleAxisAroundValue(domainAxis, -1, domainCenter);

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = combinedPlot.getSubplots();
        for (XYPlot subplot : subplots) {
          ValueAxis rangeAxis = subplot.getRangeAxis();
          if (rangeAxis != null) {
            double rangeCenter = (rangeAxis.getLowerBound() + rangeAxis.getUpperBound()) / 2;
            scaleAxisAroundValue(rangeAxis, -1, rangeCenter);
          }
        }
      } else {
        XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        ValueAxis rangeAxis = plot.getRangeAxis();

        double domainCenter = (domainAxis.getLowerBound() + domainAxis.getUpperBound()) / 2;
        double rangeCenter = (rangeAxis.getLowerBound() + rangeAxis.getUpperBound()) / 2;

        scaleAxisAroundValue(domainAxis, -1, domainCenter);
        scaleAxisAroundValue(rangeAxis, -1, rangeCenter);
      }
    }
  }

  public void resetZoom() {
    if (chartPanel != null) {
      if (chartPanel.getChart().getPlot() instanceof CombinedDomainXYPlot combinedPlot) {
        combinedPlot.getDomainAxis().setAutoRange(true);

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = combinedPlot.getSubplots();
        for (XYPlot subplot : subplots) {
          subplot.getRangeAxis().setAutoRange(true);
        }
      } else {
        XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
        plot.getDomainAxis().setAutoRange(true);
        plot.getRangeAxis().setAutoRange(true);
      }
    }
  }

  public void toggleCrosshair(boolean enabled) {
    this.crosshairEnabled = enabled;
    crosshairManager.setEnabled(enabled);
  }
}