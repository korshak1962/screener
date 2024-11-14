package korshak.com.screener.serviceImpl.chart;

import org.jfree.chart.renderer.xy.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class RendererFactory {
  private static final Color[] SMA_COLORS = {
      new Color(0, 0, 255),     // Blue
      new Color(255, 165, 0),   // Orange
      new Color(128, 0, 128),   // Purple
      new Color(0, 128, 0),     // Dark Green
      new Color(165, 42, 42),   // Brown
      new Color(64, 224, 208)   // Turquoise
  };

  private static final Color[] INDICATOR_COLORS = {
      new Color(75, 0, 130),    // Indigo
      new Color(0, 100, 0),     // Dark Green
      new Color(139, 0, 0),     // Dark Red
      new Color(25, 25, 112),   // Midnight Blue
      new Color(148, 0, 211),   // Dark Violet
      new Color(184, 134, 11),  // Dark Goldenrod
      new Color(0, 139, 139),   // Dark Cyan
      new Color(169, 169, 169), // Dark Gray
      Color.BLUE,
      Color.RED,
      Color.GREEN,
      Color.ORANGE
  };

  public XYLineAndShapeRenderer createSignalRenderer() {
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
    renderer.setSeriesShape(0, createTriangle(true));  // Buy marker
    renderer.setSeriesShape(1, createTriangle(false)); // Sell marker
    renderer.setSeriesPaint(0, Color.BLUE);
    renderer.setSeriesPaint(1, Color.MAGENTA);
    return renderer;
  }

  public XYLineAndShapeRenderer createSmaRenderer() {
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);

    for (int i = 0; i < SMA_COLORS.length; i++) {
      renderer.setSeriesPaint(i, SMA_COLORS[i]);
      renderer.setSeriesStroke(i, new BasicStroke(1.5f));
    }

    return renderer;
  }

  public XYLineAndShapeRenderer createMultipleIndicatorsRenderer(int seriesCount) {
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);

    // Create a small shape for data points
    Shape dataPoint = new java.awt.geom.Ellipse2D.Double(-2, -2, 4, 4);

    // Configure each series
    for (int i = 0; i < seriesCount; i++) {
      Color color = INDICATOR_COLORS[i % INDICATOR_COLORS.length];
      renderer.setSeriesPaint(i, color);
      renderer.setSeriesStroke(i, new BasicStroke(1.5f));
      renderer.setSeriesShape(i, dataPoint);
      renderer.setSeriesShapesVisible(i, true);
    }

    return renderer;
  }

  public TradeHistogramRenderer createTradeHistogramRenderer() {
    return new TradeHistogramRenderer(
        new Color(0, 150, 0, 180),  // Positive color
        new Color(150, 0, 0, 180)   // Negative color
    );
  }

  private Shape createTriangle(boolean pointUp) {
    Path2D.Double triangle = new Path2D.Double();
    int size = 8;

    if (pointUp) {
      triangle.moveTo(-size, size/2);
      triangle.lineTo(0, -size/2);
      triangle.lineTo(size, size/2);
    } else {
      triangle.moveTo(-size, -size/2);
      triangle.lineTo(0, size/2);
      triangle.lineTo(size, -size/2);
    }
    triangle.closePath();
    return triangle;
  }
}