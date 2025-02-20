package korshak.com.screener.serviceImpl.chart;

import java.awt.Dimension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.service.ChartService;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.Trade;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ui.ApplicationFrame;

public class ChartServiceImpl extends ApplicationFrame implements ChartService {
  private final ChartPanel chartPanel;
  private final ChartController controller;

  public ChartServiceImpl(String title) {
    super(title);
    this.controller = new ChartController();
    this.chartPanel = new ChartPanel(null); // Will be set when drawing
    initializeFrame();
  }

  private void initializeFrame() {
    setJMenuBar(new ChartMenuBar(controller));
    setContentPane(chartPanel);
  }

  @Override
  public void drawChart(List<? extends BasePrice> prices, List<? extends Signal> signals) {
    drawChart(prices, signals, Map.of(), List.of(), null);
  }

  public void drawChart(List<? extends BasePrice> prices, List<? extends Signal> signals,
                        Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators,
                        List<Trade> trades,
                        Map<String, NavigableMap<LocalDateTime, Double>> indicators) {
    ChartBuilder builder = new ChartBuilder(prices, signals, priceIndicators, trades, indicators);
    JFreeChart chart = builder.build();

    configureChartPanel(chart);
    controller.initialize(chartPanel);

    pack();
    setVisible(true);
  }

  private void configureChartPanel(JFreeChart chart) {
    chartPanel.setChart(chart);
    chartPanel.setPreferredSize(new Dimension(1000, 600));
    chartPanel.setMouseWheelEnabled(true);
    chartPanel.setZoomAroundAnchor(true);
    chartPanel.setDomainZoomable(true);
    chartPanel.setRangeZoomable(true);
  }
}