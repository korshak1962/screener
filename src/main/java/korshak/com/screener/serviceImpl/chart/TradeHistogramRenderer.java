package korshak.com.screener.serviceImpl.chart;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;

public class TradeHistogramRenderer extends AbstractXYItemRenderer {
  private final Color positiveColor;
  private final Color negativeColor;

  public TradeHistogramRenderer(Color positiveColor, Color negativeColor) {
    this.positiveColor = positiveColor;
    this.negativeColor = negativeColor;
  }

  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
                       PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis,
                       ValueAxis rangeAxis,
                       XYDataset dataset, int series, int item, CrosshairState crosshairState,
                       int pass) {

    TradeHistogramDataset histDataset = (TradeHistogramDataset) dataset;

    double x1 = histDataset.getStartX(item);
    double x2 = histDataset.getEndX(item);
    double y = dataset.getYValue(series, item);

    double startX = domainAxis.valueToJava2D(x1, dataArea, plot.getDomainAxisEdge());
    double endX = domainAxis.valueToJava2D(x2, dataArea, plot.getDomainAxisEdge());
    double zero = rangeAxis.valueToJava2D(0, dataArea, plot.getRangeAxisEdge());
    double yy = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());

    Rectangle2D bar = new Rectangle2D.Double(
        Math.min(startX, endX),
        Math.min(zero, yy),
        Math.abs(endX - startX),
        Math.abs(zero - yy)
    );

    g2.setPaint(y >= 0 ? positiveColor : negativeColor);
    g2.fill(bar);
    g2.setPaint(Color.BLACK);
    g2.draw(bar);
  }
}