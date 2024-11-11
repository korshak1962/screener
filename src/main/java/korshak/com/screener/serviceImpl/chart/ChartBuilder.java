package korshak.com.screener.serviceImpl.chart;

import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.vo.Signal;
import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import java.text.SimpleDateFormat;
import java.util.List;
import java.awt.Color;
import java.awt.Font;

public class ChartBuilder {
  private final List<? extends BasePrice> prices;
  private final List<Signal> signals;
  private final List<? extends BaseSma> smaList;
  private final DatasetFactory datasetFactory;
  private final RendererFactory rendererFactory;

  public ChartBuilder(List<? extends BasePrice> prices, List<Signal> signals,
                      List<? extends BaseSma> smaList) {
    this.prices = prices;
    this.signals = signals;
    this.smaList = smaList;
    this.datasetFactory = new DatasetFactory();
    this.rendererFactory = new RendererFactory();
  }

  public JFreeChart build() {
    JFreeChart chart = createBaseChart();
    customizePlot((XYPlot) chart.getPlot());
    return chart;
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
}