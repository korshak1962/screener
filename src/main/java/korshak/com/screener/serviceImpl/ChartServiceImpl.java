package korshak.com.screener.serviceImpl;

import java.awt.Color;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.service.ChartService;
import korshak.com.screener.vo.Signal;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.OHLCDataset;

public class ChartServiceImpl extends ApplicationFrame implements ChartService {

  public ChartServiceImpl(String title) {
    super(title);
  }

  private JFreeChart createChart(List<? extends BasePrice> prices, List<Signal> signals,
                                 List<? extends BaseSma> smaList) {
    // Create candlestick dataset
    OHLCDataset candlestickData = createCandlestickDataset(prices);

    // Create the chart
    JFreeChart chart = ChartFactory.createCandlestickChart(
        prices.getFirst().getId().getTicker(),  // title
        "Date",                      // x-axis label
        "Price",                     // y-axis label
        candlestickData,             // data
        true                         // include legend
    );

    // Get the plot and set background
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
    plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

    // Customize candlestick appearance
    CandlestickRenderer candlestickRenderer = (CandlestickRenderer) plot.getRenderer();
    candlestickRenderer.setUpPaint(Color.GREEN);
    candlestickRenderer.setDownPaint(Color.RED);
    candlestickRenderer.setDrawVolume(true);

    // Add trade markers
    addTradeMarkers(plot, signals);

    // Add SMA lines if provided
    if (smaList != null && !smaList.isEmpty()) {
      addSmaLines(plot, smaList);
    }

    return chart;
  }

  private void addSmaLines(XYPlot plot, List<? extends BaseSma> smaList) {
    // Create a new renderer for SMA lines
    XYLineAndShapeRenderer smaRenderer = new XYLineAndShapeRenderer(true, false);
    TimeSeriesCollection smaDataset = new TimeSeriesCollection();

    // Group SMAs by length to create separate lines for each length
    Map<Integer, List<BaseSma>> smasByLength = smaList.stream()
        .collect(Collectors.groupingBy(sma -> sma.getId().getLength()));

    int seriesIndex = 0;
    for (Map.Entry<Integer, List<BaseSma>> entry : smasByLength.entrySet()) {
      TimeSeries smaSeries = new TimeSeries("SMA(" + entry.getKey() + ")");

      for (BaseSma sma : entry.getValue()) {
        smaSeries.add(
            new Millisecond(java.sql.Timestamp.valueOf(sma.getId().getDate())),
            sma.getValue()
        );
      }

      smaDataset.addSeries(smaSeries);

      // Set different colors for different SMA lengths
      Color lineColor = getColorForSma(seriesIndex);
      smaRenderer.setSeriesPaint(seriesIndex, lineColor);
      smaRenderer.setSeriesStroke(seriesIndex, new BasicStroke(1.5f));

      seriesIndex++;
    }

    // Add the SMA dataset and renderer
    // Use dataset index 2 (0 is candlesticks, 1 is trade markers)
    plot.setDataset(2, smaDataset);
    plot.setRenderer(2, smaRenderer);
  }

  private Color getColorForSma(int index) {
    // Define different colors for different SMA lengths
    Color[] colors = {
        new Color(0, 0, 255),     // Blue
        new Color(255, 165, 0),   // Orange
        new Color(128, 0, 128),   // Purple
        new Color(0, 128, 0),     // Dark Green
        new Color(165, 42, 42),   // Brown
        new Color(64, 224, 208)   // Turquoise
    };
    return colors[index % colors.length];
  }

  private static OHLCDataset createCandlestickDataset(List<? extends BasePrice> prices) {
    OHLCSeries series = new OHLCSeries("Price");

    for (BasePrice price : prices) {
      series.add(
          new Millisecond(java.sql.Timestamp.valueOf(price.getId().getDate())),
          price.getOpen(),
          price.getHigh(),
          price.getLow(),
          price.getClose()
      );
    }

    OHLCSeriesCollection dataset = new OHLCSeriesCollection();
    dataset.addSeries(series);
    return dataset;
  }

  private static void addTradeMarkers(XYPlot plot, List<Signal> signals) {
    // Create triangular shapes for buy/sell markers
    Shape buyMarker = createTriangle(true);
    Shape sellMarker = createTriangle(false);

    // Create a new renderer for trade markers
    XYLineAndShapeRenderer tradeRenderer = new XYLineAndShapeRenderer(false, true);
    TimeSeriesCollection tradeDataset = new TimeSeriesCollection();
    TimeSeries buySeries = new TimeSeries("Buy");
    TimeSeries sellSeries = new TimeSeries("Sell");

    for (Signal signal : signals) {
      Millisecond timePoint = new Millisecond(
          java.sql.Timestamp.valueOf(signal.getDate())
      );

      if (signal.getAction() == 1) { // Buy
        buySeries.add(timePoint, signal.getPrice());
      } else if (signal.getAction() == -1) { // Sell
        sellSeries.add(timePoint, signal.getPrice());
      }
    }

    tradeDataset.addSeries(buySeries);
    tradeDataset.addSeries(sellSeries);

    // Add the trade markers dataset and renderer
    plot.setDataset(1, tradeDataset);
    plot.setRenderer(1, tradeRenderer);

    // Configure trade markers appearance
    tradeRenderer.setSeriesShape(0, buyMarker);
    tradeRenderer.setSeriesShape(1, sellMarker);
    tradeRenderer.setSeriesPaint(0, Color.BLUE);
    tradeRenderer.setSeriesPaint(1, Color.MAGENTA);
  }

  private static Shape createTriangle(boolean pointUp) {
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

  @Override
  public void drawChart(List<? extends BasePrice> prices, List<Signal> signals) {
    drawChart(prices, signals, null);
  }

  public void drawChart(List<? extends BasePrice> prices, List<Signal> signals,
                        List<? extends BaseSma> smaList) {
    JFreeChart chart = createChart(prices, signals, smaList);
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(1000, 600));
    setContentPane(chartPanel);
    pack();
    setVisible(true);
  }
}