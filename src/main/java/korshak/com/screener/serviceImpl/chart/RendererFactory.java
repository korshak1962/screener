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