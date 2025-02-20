package korshak.com.screener.serviceImpl.chart;

import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.Trade;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class ChartBuilder {
  private final List<? extends BasePrice> prices;
  private final List<? extends Signal> signals;
  private final Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators;
  private final List<Trade> tradesLong;
  private final Map<String, NavigableMap<LocalDateTime, Double>> indicators;
  private final DatasetFactory datasetFactory;
  private final RendererFactory rendererFactory;

  public ChartBuilder(List<? extends BasePrice> prices, List<? extends Signal> signals,
                      Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators,
                      List<Trade> tradesLong,
                      Map<String, NavigableMap<LocalDateTime, Double>> indicators) {
    this.prices = prices;
    this.signals = signals;
    this.priceIndicators = priceIndicators;
    this.tradesLong = tradesLong;
    this.indicators = indicators;
    this.datasetFactory = new DatasetFactory();
    this.rendererFactory = new RendererFactory();
  }

  public JFreeChart build() {
    JFreeChart candlestickChart = createBaseChart();
    XYPlot mainPlot = (XYPlot) candlestickChart.getPlot();
    customizePlot(mainPlot);

    // Create histogram subplot
    XYPlot histogramPlot = null;
    if (tradesLong != null && !tradesLong.isEmpty()) {
      histogramPlot = createHistogramPlot();
    }

    // Create indicators subplot
    XYPlot indicatorsPlot = null;
    if (indicators != null && !indicators.isEmpty()) {
      indicatorsPlot = createIndicatorsPlot();
    }

    // If we have additional plots, create a combined plot
    if (histogramPlot != null || indicatorsPlot != null) {
      CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(mainPlot.getDomainAxis());
      combinedPlot.add(mainPlot, 6);  // Main plot gets 6 parts

      if (histogramPlot != null) {
        combinedPlot.add(histogramPlot, 2);  // Histogram gets 2 parts
      }

      if (indicatorsPlot != null) {
        combinedPlot.add(indicatorsPlot, 2);  // Indicators get 2 parts
      }

      combinedPlot.setGap(8.0);
      combinedPlot.setBackgroundPaint(Color.WHITE);

      return new JFreeChart(
          prices.getFirst().getId().getTicker(),
          JFreeChart.DEFAULT_TITLE_FONT,
          combinedPlot,
          true
      );
    }

    return candlestickChart;
  }

  private JFreeChart createBaseChart() {
    return ChartFactory.createCandlestickChart(
        prices.getFirst().getId().getTicker(),
        "Date",
        "Price",
        datasetFactory.createCandlestickDataset(prices),
        true
    );
  }

  private void customizePlot(XYPlot plot) {
    configureDateAxis(plot);
    configureAppearance(plot);
    addDatasets(plot);
  }

  private void configureDateAxis(XYPlot plot) {
    DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
    dateAxis.setDateFormatOverride(new SimpleDateFormat("dd MMM yyyy"));
    dateAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
  }

  private void configureAppearance(XYPlot plot) {
    plot.setBackgroundPaint(Color.WHITE);
    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
    plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
  }

  private void addDatasets(XYPlot plot) {
    if (signals != null && !signals.isEmpty()) {
      plot.setDataset(1, datasetFactory.createSignalDataset(signals));
      plot.setRenderer(1, rendererFactory.createSignalRenderer());
    }

    if (priceIndicators != null && !priceIndicators.isEmpty()) {
      TimeSeriesCollection indicatorsDataset = new TimeSeriesCollection();

      for (Map.Entry<String, NavigableMap<LocalDateTime, Double>> indicator : priceIndicators.entrySet()) {
        TimeSeries series = new TimeSeries(indicator.getKey());

        for (Map.Entry<LocalDateTime, Double> entry : indicator.getValue().entrySet()) {
          LocalDateTime dateTime = entry.getKey();
          Millisecond timePeriod = new Millisecond(
              java.util.Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
          );
          series.add(timePeriod, entry.getValue());
        }

        indicatorsDataset.addSeries(series);
      }

      plot.setDataset(2, indicatorsDataset);
      plot.setRenderer(2, rendererFactory.createMultipleIndicatorsRenderer(priceIndicators.size()));
    }
  }

  private XYPlot createHistogramPlot() {
    TradeHistogramDataset dataset = datasetFactory.createTradeHistogramDataset(tradesLong);
    TradeHistogramRenderer renderer = rendererFactory.createTradeHistogramRenderer();

    NumberAxis rangeAxis = new NumberAxis("PnL");
    rangeAxis.setAutoRangeIncludesZero(true);
    XYPlot plot = new XYPlot(dataset, null, rangeAxis, renderer);

    plot.setBackgroundPaint(Color.WHITE);
    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
    plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

    return plot;
  }

  private XYPlot createIndicatorsPlot() {
    TimeSeriesCollection dataset = new TimeSeriesCollection();

    for (Map.Entry<String, NavigableMap<LocalDateTime, Double>> indicator : indicators.entrySet()) {
      TimeSeries series = new TimeSeries(indicator.getKey());

      for (Map.Entry<LocalDateTime, Double> entry : indicator.getValue().entrySet()) {
        LocalDateTime dateTime = entry.getKey();
        Millisecond timePeriod = new Millisecond(
            java.util.Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
        );
        series.add(timePeriod, entry.getValue());
      }

      dataset.addSeries(series);
    }

    // Create plot
    NumberAxis rangeAxis = new NumberAxis("Indicators");
    rangeAxis.setAutoRangeIncludesZero(false);

    // Create custom renderer for multiple lines
    XYLineAndShapeRenderer renderer =
        rendererFactory.createMultipleIndicatorsRenderer(indicators.size());

    XYPlot plot = new XYPlot(dataset, null, rangeAxis, renderer);

    // Customize appearance
    plot.setBackgroundPaint(Color.WHITE);
    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
    plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

    return plot;
  }
}