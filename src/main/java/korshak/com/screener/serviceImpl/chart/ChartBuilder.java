package korshak.com.screener.serviceImpl.chart;

import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.Trade;
import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import java.text.SimpleDateFormat;
import java.util.List;
import java.awt.Color;
import java.awt.Font;

public class ChartBuilder {
  private final List<? extends BasePrice> prices;
  private final List<Signal> signals;
  private final List<? extends BaseSma> smaList;
  private final List<Trade> tradesLong;
  private final DatasetFactory datasetFactory;
  private final RendererFactory rendererFactory;

  public ChartBuilder(List<? extends BasePrice> prices, List<Signal> signals,
                      List<? extends BaseSma> smaList, List<Trade> tradesLong) {
    this.prices = prices;
    this.signals = signals;
    this.smaList = smaList;
    this.tradesLong = tradesLong;
    this.datasetFactory = new DatasetFactory();
    this.rendererFactory = new RendererFactory();
  }

  public JFreeChart build() {
    JFreeChart candlestickChart = createBaseChart();
    XYPlot mainPlot = (XYPlot) candlestickChart.getPlot();
    customizePlot(mainPlot);

    if (tradesLong != null && !tradesLong.isEmpty()) {
      XYPlot histogramPlot = createHistogramPlot();

      CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(mainPlot.getDomainAxis());
      combinedPlot.add(mainPlot, 3);
      combinedPlot.add(histogramPlot, 1);

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

    if (smaList != null && !smaList.isEmpty()) {
      plot.setDataset(2, datasetFactory.createSmaDataset(smaList));
      plot.setRenderer(2, rendererFactory.createSmaRenderer());
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
}