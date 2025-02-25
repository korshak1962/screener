package korshak.com.screener.serviceImpl;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import korshak.com.screener.dao.OptParam;
import korshak.com.screener.dao.OptParamDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.Trend;
import korshak.com.screener.service.ChartService;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.service.TrendService;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.serviceImpl.chart.ChartServiceImpl;
import korshak.com.screener.serviceImpl.strategy.Optimizator;
import korshak.com.screener.serviceImpl.strategy.OptimizatorTilt;
import korshak.com.screener.serviceImpl.strategy.StrategyMerger;
import korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy;
import korshak.com.screener.utils.ExcelExportService;
import korshak.com.screener.vo.StrategyResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class Reporter {

  private static final String PRICE_TO_BUY = "priceToBuy";
  private static final String PRICE_TO_SELL = "priceToSell";
  private static final String TICKER = "Ticker";
  private static final String CLOSE = "Close";
  private static final String FINVIZ_URL = "F_URL";
  private final OptimizatorTilt optimizatorTilt;
  private final StrategyMerger strategyMerger;
  private final FuturePriceByTiltCalculator futurePriceByTiltCalculator;
  private final TradeService tradeService;
  private final TiltFromBaseStrategy tiltFromBaseStrategy;
  private final Strategy buyAndHoldStrategy;
  private final TrendService trendService;
  private final OptParamDao optParamDao;
  public Map<String, StrategyResult> tickerToResult = new HashMap<>();

  public Reporter(@Qualifier("OptimizatorTilt") OptimizatorTilt optimizatorTilt,
                  @Qualifier("StrategyMerger") StrategyMerger strategyMerger,
                  FuturePriceByTiltCalculator futurePriceByTiltCalculator,
                  TradeService tradeService,
                  @Qualifier("TiltFromBaseStrategy") TiltFromBaseStrategy tiltFromBaseStrategy,
                  @Qualifier("BuyAndHoldStrategy") Strategy buyAndHoldStrategy,
                  TrendService trendService,
                  OptParamDao optParamDao) {
    this.optimizatorTilt = optimizatorTilt;
    this.strategyMerger = strategyMerger;
    this.futurePriceByTiltCalculator = futurePriceByTiltCalculator;
    this.tradeService = tradeService;
    this.tiltFromBaseStrategy = tiltFromBaseStrategy;
    this.buyAndHoldStrategy = buyAndHoldStrategy;
    this.trendService = trendService;
    this.optParamDao = optParamDao;
  }

  private static void show(Strategy strategy, StrategyResult strategyResultTilt) {
    System.setProperty("java.awt.headless", "false");
    ChartService chartService = new ChartServiceImpl(strategy.getStrategyName());
    chartService.drawChart(strategyResultTilt.getPrices(), strategyResultTilt.getSignals()
        , strategy.getPriceIndicators()
        , strategyResultTilt.getTradesLong(), strategy.getIndicators());
    pause();
  }

  static void pause() {
    try {
      System.out.println("Press any key to continue...");
      System.in.read();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static List<OptParam> getOptParamsAsList(String ticker, TimeFrame timeFrame,
                                                   Map<String, Double> optParams) {
    List<OptParam> optParamsList = new ArrayList<>();
    for (Map.Entry<String, Double> entry : optParams.entrySet()) {
      optParamsList.add(new OptParam(ticker, entry.getKey(), timeFrame, entry.getValue()));
    }
    return optParamsList;
  }

  public void createExcelReport(List<String> tickers, LocalDateTime startDate,
                                LocalDateTime endDate, TimeFrame timeFrame) {
    Map<String, StrategyResult> res = reportForList(tickers, startDate,
        endDate, timeFrame);

    Map<String, List<String>> colnameToValues = new LinkedHashMap<>();
    colnameToValues.put(TICKER, new ArrayList<>());
    colnameToValues.put(CLOSE, new ArrayList<>());
    colnameToValues.put(PRICE_TO_BUY, new ArrayList<>());
    colnameToValues.put(PRICE_TO_SELL, new ArrayList<>());
    DecimalFormat df = new DecimalFormat("#.##");
    for (TimeFrame timeFrameTrend : TimeFrame.values()) {
      if (timeFrameTrend != TimeFrame.MIN5) { // Skip 5-minute timeframe as it's the base
        colnameToValues.put(timeFrameTrend + "_trend", new ArrayList<>());
      }
    }
    colnameToValues.put(FINVIZ_URL, new ArrayList<>());
    Set<String> urlColumns = new HashSet<>();
    urlColumns.add(FINVIZ_URL);
    for (Map.Entry<String, StrategyResult> entry : res.entrySet()) {
      StrategyResult strategyResult = entry.getValue();

      colnameToValues.get(TICKER).add(entry.getKey());
      colnameToValues.get(CLOSE)
          .add(Double.valueOf(strategyResult.getPrices().getLast().getClose()).toString());
      for (TimeFrame timeFrameTrend : TimeFrame.values()) {
        if (timeFrameTrend != TimeFrame.MIN5) { // Skip 5-minute timeframe as it's the base
          Trend trend =
              trendService.findLatestTrendBeforeDate(entry.getKey(), timeFrameTrend, endDate);
          String trendString;
          if (trend != null) {
            trendString = trend.toString();
            // Do something with trendString
          } else {
            // Handle the null case
            trendString = "No trend found";
          }

          colnameToValues.get(timeFrameTrend + "_trend").add(trendString);
        }
      }
      colnameToValues.get(PRICE_TO_BUY).add(df.format(
          strategyResult.getOptParams().get(PRICE_TO_BUY)));
      colnameToValues.get(PRICE_TO_SELL).add(df.format(
          strategyResult.getOptParams().get(PRICE_TO_SELL)));
      colnameToValues.get(FINVIZ_URL).add(buildFinvizUrl(entry.getKey()));
    }
    try {
      ExcelExportService.reportForMap("tiltStratRes.xlsx", "results",
          colnameToValues, urlColumns);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    System.out.println("Report created");
  }

  public Map<String, StrategyResult> reportForList(List<String> tickers, LocalDateTime startDate,
                                                   LocalDateTime endDate, TimeFrame timeFrame) {
    for (String ticker : tickers) {
      try {
        trendService.calculateAndStorePriceTrendForAllTimeframes(ticker);
        tickerToResult.put(ticker, opt(ticker, startDate, endDate, timeFrame));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return tickerToResult;
  }

  public StrategyResult opt(String ticker, LocalDateTime startDate, LocalDateTime endDate,
                            TimeFrame timeFrame) throws IOException {

    Map<String, Double> optParams = getOptParams(ticker, startDate, endDate, timeFrame);
    optParamDao.saveAll(getOptParamsAsList(ticker, timeFrame, optParams));
    StrategyResult strategyResult =
        getStrategyResult(ticker, startDate, endDate, timeFrame, optParams);
    futurePriceCalc(ticker, optParams);
    strategyResult.setOptParams(optParams);
    return strategyResult;
  }

  private StrategyResult getStrategyResult(String ticker, LocalDateTime startDate,
                                           LocalDateTime endDate, TimeFrame timeFrame,
                                           Map<String, Double> optParams) throws IOException {
    strategyMerger
        .setStopLossPercent(optParams.get(Optimizator.STOP_LOSS))
        .init(ticker, timeFrame, startDate, endDate)
        //  .addStrategy(
        //      stopLossLessThanPrevMinExtremumStrategy.init(ticker, TimeFrame.DAY, startDate, endDate))
        .addStrategy(
            initStrategy(tiltFromBaseStrategy, timeFrame, ticker, startDate, endDate, optParams)
        )
        .mergeSignals()
    ;
    StrategyResult strategyResult = evaluateStrategy(strategyMerger);
    return strategyResult;
  }

  Map<String, Double> getOptParamsAsMap(List<OptParam> optParamList) {
    Map<String, Double> optParams = new HashMap<>();
    for (OptParam optParam : optParamList) {
      optParams.put(optParam.getParam(), optParam.getValue());
    }
    return optParams;
  }


  public StrategyResult readAndShow(String ticker, LocalDateTime startDate, LocalDateTime endDate,
                                    TimeFrame timeFrame) throws IOException {
    Map<String, Double> optParams = readOptParams(ticker, timeFrame);
    StrategyResult strategyResult =
        getStrategyResult(ticker, startDate, endDate, timeFrame, optParams);
    show(strategyMerger, strategyResult);
    return strategyResult;
  }

  public StrategyResult optAndShow(String ticker, LocalDateTime startDate, LocalDateTime endDate,
                                   TimeFrame timeFrame) throws IOException {
    StrategyResult strategyResult = opt(ticker, startDate, endDate, timeFrame);
    show(strategyMerger, strategyResult);
    return strategyResult;
  }

  private Map<String, Double> readOptParams(String ticker, TimeFrame timeFrame) {
    List<OptParam> optParamList = optParamDao.getAllForTickerAndTimeframe(ticker, timeFrame);
    Map<String, Double> optParams = getOptParamsAsMap(optParamList);
    return optParams;
  }

  private Map<String, Double> getOptParams(String ticker, LocalDateTime startDate,
                                           LocalDateTime endDate, TimeFrame timeFrame) {
    int minLength = 3;
    int maxLength = 10;
    int stepLength = 1;
    double minTiltBuy = -0.01;
    double maxTiltBuy = 0.02;
    double tiltBuyStep = 0.01;
    double minTiltSell = -0.05;
    double maxTiltSell = 0.01;
    double tiltSellStep = 0.01;
    optimizatorTilt.configure(minLength, maxLength, stepLength, minTiltBuy, maxTiltBuy, tiltBuyStep,
        minTiltSell, maxTiltSell, tiltSellStep);
    double minStopLossPercent = .95;
    double maxStopLossPercent = .99;
    double stepOfStopLoss = 0.005;
    Map<String, Double> optParams =
        optimazeStrategy(optimizatorTilt, ticker, timeFrame, startDate, endDate,
            minStopLossPercent,
            maxStopLossPercent, stepOfStopLoss);
    return optParams;
  }

  private Map<String, Double> optimazeStrategy(Optimizator optimizator, String ticker,
                                               TimeFrame timeFrame,
                                               LocalDateTime startDate, LocalDateTime endDate,
                                               double minPercent,
                                               double maxPercent, double step) {
    optimizator.init(ticker, timeFrame, startDate, endDate);
    Map<String, Double> params =
        optimizator.findOptimumParametersWithStopLoss(minPercent, maxPercent, step);
    System.out.println(" " + ticker + " " + params.remove(Optimizator.MAX_PNL) + " " + params);
    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdownLong(buyAndHoldStrategy, ticker,
            startDate,
            endDate,
            timeFrame);
    System.out.println("Buy and hold pnl = " + buyAndHoldstrategyResult.getLongPnL());
    //System.exit(0);
    return params;
  }

  private void futurePriceCalc(String ticker, Map<String, Double> optParams) {
    double priceToBuy = futurePriceByTiltCalculator.calculatePriceBinary(ticker, TimeFrame.DAY,
        optParams.get(OptimizatorTilt.LENGTH).intValue(), optParams.get(OptimizatorTilt.TILT_BUY));
    System.out.println("Binary Price to buy: " + priceToBuy);
    //priceToBuy = futurePriceByTiltCalculator.calculatePrice(ticker, TimeFrame.DAY,
    //    optParams.get(OptimizatorTilt.LENGTH).intValue(), optParams.get(OptimizatorTilt.TILT_BUY));
    // System.out.println("Old Price to buy: " + priceToBuy);

    double priceToSell = futurePriceByTiltCalculator.calculatePriceBinary(ticker, TimeFrame.DAY,
        optParams.get(OptimizatorTilt.LENGTH).intValue(), optParams.get(OptimizatorTilt.TILT_SELL));
    System.out.println("Binary Price to sell: " + priceToSell);
    optParams.put(PRICE_TO_BUY, priceToBuy);
    optParams.put(PRICE_TO_SELL, priceToSell);
  }

  private StrategyResult evaluateStrategy(Strategy strategy) throws IOException {
    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdownLong(buyAndHoldStrategy, strategy.getTicker(),
            strategy.getStartDate(),
            strategy.getEndDate(),
            strategy.getTimeFrame());
    //StrategyResult strategyResultTilt =
    //    tradeService.calculateProfitAndDrawdownLong(tiltStrategy, ticker, timeFrame);

    StrategyResult strategyResultTilt =
        tradeService.calculateProfitAndDrawdownLong(strategy, strategy.getTicker(),
            strategy.getStartDate(),
            strategy.getEndDate(),
            strategy.getTimeFrame());
    System.out.println(strategy.getStrategyName() + " result: " + strategyResultTilt);
    System.out.println(
        buyAndHoldStrategy.getStrategyName() + " result: " + buyAndHoldstrategyResult);

/*
    ExcelExportService.exportTradesToExcel(strategyResultTilt.getTradesLong(),
        "trades_long.xlsx");

    ExcelExportService.exportTradesToExcel(strategyResultTilt.getTradesShort(),
        "trades_short.xlsx");

   */
    // show(strategy, strategyResultTilt);
    /*chartService.drawChart(strategyResultTilt.getPrices(), strategyResultTilt.getSignalsLong()
        , ((TiltStrategy) tiltStrategy).getSmaList()
        , strategyResultTilt.getTradesLong());
     */
    //chartService.drawChart(strategyResult.getPrices(),strategyResult.getSignalsLong());Google01@vs7f20

    return strategyResultTilt;
  }

  private TiltFromBaseStrategy initStrategy(TiltFromBaseStrategy tiltStrategy, TimeFrame timeFrame,
                                            String ticker,
                                            LocalDateTime startDate,
                                            LocalDateTime endDate,
                                            Map<String, Double> params
  ) {
    tiltStrategy.init(ticker, timeFrame, startDate, endDate);
    //{Length=44.0, TiltBuy=0.01, TiltSell=-0.05}
    //tiltStrategy.setLength(9);
    tiltStrategy.setLength(params.get(OptimizatorTilt.LENGTH).intValue());
    //tiltStrategy.setTiltBuy(0.02);
    tiltStrategy.setTiltBuy(params.get(OptimizatorTilt.TILT_BUY));
    //tiltStrategy.setTiltSell(-0.02);
    tiltStrategy.setTiltSell(params.get(OptimizatorTilt.TILT_SELL));
    tiltStrategy.calcSignals();
    return tiltStrategy;
  }

  public String buildFinvizUrl(String ticker) {
    return String.format("https://finviz.com/quote.ashx?t=%s&ta=1&p=d&ty=ea", ticker.toUpperCase());
  }
}
