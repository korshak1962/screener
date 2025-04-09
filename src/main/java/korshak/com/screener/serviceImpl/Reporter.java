package korshak.com.screener.serviceImpl;

import static korshak.com.screener.serviceImpl.strategy.StrategyMerger.END_DATE;
import static korshak.com.screener.serviceImpl.strategy.StrategyMerger.START_DATE;
import static korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy.LENGTH;
import static korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy.TILT_BUY;
import static korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy.TILT_SELL;
import static korshak.com.screener.utils.Utils.getOptParamsAsMap;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import korshak.com.screener.dao.Param;
import korshak.com.screener.dao.OptParamDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.Trend;
import korshak.com.screener.service.ChartService;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.service.TrendService;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.serviceImpl.chart.ChartServiceImpl;
import korshak.com.screener.serviceImpl.strategy.BaseStrategy;
import korshak.com.screener.serviceImpl.strategy.GenericOptimizator;
import korshak.com.screener.serviceImpl.strategy.StrategyMerger;
import korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy;
import korshak.com.screener.serviceImpl.strategy.TrendChangeStrategy;
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
  private static final String TREND = "_trend";
  private static final String PREVIOUS = "_Previous";
  private static final String STRATAGY_RESULT = "StratRes";
  public static double STOP_LOSS_MAX_PERCENT = .97;
  private final StrategyProvider strategyProvider;
  private final GenericOptimizator genericOptimizator;
  private final StrategyMerger strategyMerger;
  private final FuturePriceByTiltCalculator futurePriceByTiltCalculator;
  private final TradeService tradeService;
  private final TiltFromBaseStrategy tiltFromBaseStrategy;
  private final Strategy buyAndHoldStrategy;
  private final TrendService trendService;
  private final OptParamDao optParamDao;
  private final TrendChangeStrategy trendChangeStrategy;
  public Map<String, StrategyResult> tickerToResult = new HashMap<>();

  public Reporter(StrategyProvider strategyProvider,
                  @Qualifier("GenericOptimizator") GenericOptimizator genericOptimizator,
                  @Qualifier("StrategyMerger") StrategyMerger strategyMerger,
                  FuturePriceByTiltCalculator futurePriceByTiltCalculator,
                  TradeService tradeService,
                  @Qualifier("TiltFromBaseStrategy") TiltFromBaseStrategy tiltFromBaseStrategy,
                  @Qualifier("BuyAndHoldStrategy") Strategy buyAndHoldStrategy,
                  TrendService trendService,
                  OptParamDao optParamDao,
                  @Qualifier("TrendChangeStrategy") TrendChangeStrategy trendChangeStrategy) {
    this.strategyProvider = strategyProvider;
    this.strategyMerger = strategyMerger;
    this.futurePriceByTiltCalculator = futurePriceByTiltCalculator;
    this.tradeService = tradeService;
    this.tiltFromBaseStrategy = tiltFromBaseStrategy;
    this.buyAndHoldStrategy = buyAndHoldStrategy;
    this.trendService = trendService;
    this.optParamDao = optParamDao;
    this.trendChangeStrategy = trendChangeStrategy;
    this.genericOptimizator = genericOptimizator;
  }

  private static void show(StrategyResult strategyResult,
                           Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators,
                           Map<String, NavigableMap<LocalDateTime, Double>> indicators,
                           String chartName) {
    System.setProperty("java.awt.headless", "false");
    ChartService chartService = new ChartServiceImpl(chartName);
    chartService.drawChart(strategyResult.getPrices(), strategyResult.getSignals()
        , priceIndicators
        , strategyResult.getTradesLong(), indicators);
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

  private static List<Param> fillOptParamList(List<Param> optParamList,
                                              Map<String, Double> paramToDouble,
                                              Map<String, String> paramToString) {
    for (Param param : optParamList) {
      Double value = paramToDouble.get(param.getId().getParam());
      if (value != null) {
        param.setValue(value);
      }
      String strValue = paramToString.get(param.getId().getParam());
      if (strValue != null) {
        param.setValueString(strValue);
      }
    }
    return optParamList;
  }

  public void createExcelReport(List<String> tickers, LocalDateTime startDate,
                                LocalDateTime endDate, TimeFrame timeFrame, String fileName) {
    Map<String, StrategyResult> res = reportForList(tickers, startDate,
        endDate, timeFrame);
    Map<String, List<String>> colnameToValues = new LinkedHashMap<>();
    colnameToValues.put(TICKER, new ArrayList<>());
    colnameToValues.put(CLOSE, new ArrayList<>());
    colnameToValues.put(PRICE_TO_BUY, new ArrayList<>());
    colnameToValues.put(PRICE_TO_SELL, new ArrayList<>());
    colnameToValues.put(STRATAGY_RESULT, new ArrayList<>());
    DecimalFormat df = new DecimalFormat("#.##");
    for (TimeFrame timeFrameTrend : TimeFrame.values()) {
      if (timeFrameTrend != TimeFrame.MIN5) { // Skip 5-minute timeframe as it's the base
        colnameToValues.put(timeFrameTrend + TREND, new ArrayList<>());
        colnameToValues.put(timeFrameTrend + PREVIOUS, new ArrayList<>());
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
          Trend trendChange =
              trendService.findLatestTrendChangeBefore(entry.getKey(), timeFrameTrend, trend);
          String trendString;
          int closeTrend;
          if (trend != null) {
            trendString = trend.toString();
            closeTrend = (strategyResult.getPrices().getLast().getClose() >
                strategyResult.getPrices().get(strategyResult.getPrices().size() - 2).getClose()) ?
                1 : -1;
            trendString += "," + closeTrend;
          } else {
            trendString = "No trend found";
          }
          String trendChangeString;
          if (trendChange != null) {
            trendChangeString = trendChange.toString();
          } else {
            trendChangeString = "No change found";
          }
          colnameToValues.get(timeFrameTrend + TREND).add(trendString);
          colnameToValues.get(timeFrameTrend + PREVIOUS).add(trendChangeString);
        }
      }
      if (strategyResult.getOptParams() == null) {
        System.out.println("==== Opt params not found for " + entry.getKey());
        continue;
      }
      colnameToValues.get(PRICE_TO_BUY).add(df.format(
          strategyResult.getOptParams().get(PRICE_TO_BUY)));
      colnameToValues.get(PRICE_TO_SELL).add(df.format(
          strategyResult.getOptParams().get(PRICE_TO_SELL)));
      colnameToValues.get(STRATAGY_RESULT).add(strategyResult.toExcelString());
      colnameToValues.get(FINVIZ_URL).add(buildFinvizUrl(entry.getKey()));
    }
    try {
      ExcelExportService.reportForMap(fileName + ".xlsx", "results",
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
        tickerToResult.put(ticker,
            readParamsGetStrategyResult(ticker, startDate, endDate, timeFrame));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return tickerToResult;
  }

  public StrategyResult getStrategyResult(List<Strategy> subStrategies, String ticker,
                                          LocalDateTime startDate,
                                          LocalDateTime endDate, TimeFrame timeFrame) {
    strategyMerger.getSubStrategies().clear();
    strategyMerger
        .addStrategies(
            subStrategies
        )
        .setStopLossPercent(STOP_LOSS_MAX_PERCENT)
        .init(ticker, timeFrame, startDate, endDate)
        .mergeSignals();
    return evaluateStrategy(strategyMerger);
  }

  public StrategyResult getStrategyResult(BaseStrategy baseStrategy, String ticker,
                                          LocalDateTime startDate,
                                          LocalDateTime endDate, TimeFrame timeFrame) {
    strategyMerger
        .addStrategy(
            baseStrategy, timeFrame
        )
        .setStopLossPercent(STOP_LOSS_MAX_PERCENT)
        .init(ticker, timeFrame, startDate, endDate)
        .mergeSignals();
    return evaluateStrategy(strategyMerger);
  }

  public StrategyResult getStrategyResult(String ticker, LocalDateTime startDate,
                                          LocalDateTime endDate, TimeFrame timeFrame) {

    BaseStrategy baseStrategy =
        initStrategy(tiltFromBaseStrategy, timeFrame, ticker, startDate, endDate);
    List<Strategy> strategies = new ArrayList<>();
    strategies.add(baseStrategy);
    return getStrategyResult(ticker, startDate, endDate, timeFrame,
        STOP_LOSS_MAX_PERCENT, strategies);
  }

  public StrategyResult readParamsGetStrategyResult(String ticker, LocalDateTime startDate,
                                                    LocalDateTime endDate, TimeFrame timeFrame,
                                                    List<String> strategies) {
    strategyMerger.getSubStrategies().clear();
    strategyMerger.init(ticker, timeFrame, startDate, endDate);
    strategyProvider.init(ticker, startDate, endDate, timeFrame);
    for (String strategyName : strategies) {
      List<Param> params = optParamDao
          .findValuesByTickerAndTimeframeAndStrategy(ticker, timeFrame, strategyName);
      Strategy strategy = strategyProvider.getStrategy(strategyName);
      strategy.configure(getOptParamsAsMap(params));
      //  strategyMerger.configure(getOptParamsAsMap(params));
      strategyMerger.addStrategy(strategy, timeFrame);
    }
    List<Param> params = optParamDao
        .findValuesByTickerAndTimeframeAndStrategy(ticker, timeFrame,
            strategyMerger.getClass().getSimpleName());
    strategyMerger.configure(getOptParamsAsMap(params));

    strategyMerger.mergeSignals();
    StrategyResult strategyResult = evaluateStrategy(strategyMerger);
    show(strategyResult, strategyMerger.getPriceIndicators(),
        strategyMerger.getIndicators(), strategyMerger.getStrategyName());
    System.out.println(strategyResult);
    return strategyResult;
  }

  private StrategyResult getStrategyResult(String ticker, LocalDateTime startDate,
                                           LocalDateTime endDate, TimeFrame timeFrame,
                                           double stopLossMaxPercent,
                                           List<Strategy> strategies) {

    strategyMerger
        .setStopLossPercent(stopLossMaxPercent)
        .init(ticker, timeFrame, startDate, endDate);
    Map<String, Double> optParams = new HashMap<>();
    for (Strategy strategy : strategies) {
      strategyMerger.addStrategy(strategy, timeFrame);
      Map<String, Param> optParamsMap = strategy.getParams();
      for (Map.Entry<String, Param> entry : optParamsMap.entrySet()) {
        optParams.put(entry.getKey(), entry.getValue().getValue());
      }
    }
    strategyMerger.mergeSignals();
    StrategyResult strategyResult = evaluateStrategy(strategyMerger);
    strategyResult.setOptParams(optParams);
    return strategyResult;
  }

  public StrategyResult readAndShow(Map<TimeFrame, List<String>> timeFrameToStrategyNames,
                                    String ticker, LocalDateTime startDate,
                                    LocalDateTime endDate) {
    List<Strategy> strategies =
        getStrategies(timeFrameToStrategyNames, ticker, startDate, endDate);
    TimeFrame signalTimeFrame = Collections.min(timeFrameToStrategyNames.keySet());
    StrategyResult strategyResult =
        getStrategyResult(ticker, startDate, endDate, signalTimeFrame, STOP_LOSS_MAX_PERCENT,
            strategies);
    show(strategyResult, strategyMerger.getPriceIndicators(),
        strategyMerger.getIndicators(), strategyMerger.getStrategyName());
    return strategyResult;
  }

  private List<Strategy> getStrategies(Map<TimeFrame, List<String>> timeFrameToStrategyNames,
                                       String ticker, LocalDateTime startDate,
                                       LocalDateTime endDate) {
    List<Strategy> strategies = new ArrayList<>();
    for (Map.Entry<TimeFrame, List<String>> entry : timeFrameToStrategyNames.entrySet()) {
      strategyProvider.init(ticker, startDate, endDate, entry.getKey());
      for (String strategyName : entry.getValue()) {
        Strategy strategy = strategyProvider.getStrategy(strategyName);
        strategies.add(strategy);
      }
    }
    return strategies;
  }

  public StrategyResult readAndShow(String ticker, LocalDateTime startDate, LocalDateTime endDate,
                                    TimeFrame timeFrame) throws IOException {
    StrategyResult strategyResult =
        readParamsGetStrategyResult(ticker, startDate, endDate, timeFrame);
    show(strategyResult, strategyMerger.getPriceIndicators(),
        strategyMerger.getIndicators(), strategyMerger.getStrategyName());
    return strategyResult;
  }

  public StrategyResult evaluateAndShow(List<Strategy> subStrategies,
                                        String ticker, LocalDateTime startDate,
                                        LocalDateTime endDate,
                                        TimeFrame timeFrame) throws IOException {
    StrategyResult strategyResult =
        getStrategyResult(subStrategies, ticker, startDate, endDate, timeFrame);
    show(strategyResult, strategyMerger.getPriceIndicators(),
        strategyMerger.getIndicators(), strategyMerger.getStrategyName());
    return strategyResult;
  }

  public StrategyResult evaluateAndShow(BaseStrategy baseStrategy,
                                        String ticker, LocalDateTime startDate,
                                        LocalDateTime endDate,
                                        TimeFrame timeFrame) throws IOException {
    baseStrategy.init(ticker, timeFrame, startDate, endDate);
    StrategyResult strategyResult =
        getStrategyResult(baseStrategy, ticker, startDate, endDate, timeFrame);
    show(strategyResult, strategyMerger.getPriceIndicators(),
        strategyMerger.getIndicators(), strategyMerger.getStrategyName());
    return strategyResult;
  }

  private StrategyResult readParamsGetStrategyResult(String ticker, LocalDateTime startDate,
                                                     LocalDateTime endDate, TimeFrame timeFrame)
      throws IOException {
    StrategyResult strategyResult =
        getStrategyResult(ticker, startDate, endDate, timeFrame);
    futurePriceCalc(ticker, strategyResult.getOptParams());
    return strategyResult;
  }


  private Map<String, Double> readOptParams(String ticker, TimeFrame timeFrame) {
    Map<String, Double> optParams = optParamDao.findValuesByTickerAndTimeframe(ticker, timeFrame);
    return optParams;
  }

  public void findResultFor2strategies(String ticker,
                                       LocalDateTime startDate,
                                       LocalDateTime endDate,
                                       TimeFrame timeFrameHigh,
                                       TimeFrame timeframeLow) {

    strategyProvider.init(ticker, startDate, endDate, timeframeLow);
    List<Strategy> subStrategies = new ArrayList<>();

    Strategy strategyHigh = strategyProvider.getStrategy(TrendChangeStrategy.class);
    //strategyHigh.configure();timeFrameHigh
    subStrategies.add(strategyHigh);
    Strategy strategyLow = strategyProvider.getStrategy(TrendChangeStrategy.class);
    //strategyLow.configure(); timeframeLow
    subStrategies.add(strategyLow);
    StrategyResult strategyResult =
        getStrategyResult(subStrategies, ticker, startDate, endDate, timeframeLow);
    show(strategyResult,
        strategyMerger.getPriceIndicators(), strategyMerger.getIndicators(),
        strategyMerger.getStrategyName());
  }


  public Map<Strategy, Map<String, Param>> findOptParamAndSaveGeneric(String ticker,
                                                                      LocalDateTime startDate,
                                                                      LocalDateTime endDate,
                                                                      TimeFrame timeFrame,
                                                                      String caseId) {
    Param stopLossParam = new Param(
        ticker,
        StrategyMerger.STOP_LOSS_PERCENT,
        strategyMerger.getClass().getSimpleName(),
        caseId,
        timeFrame,
        strategyMerger.getClass().getSimpleName(),
        0.9,  // Start value
        "",
        0.9f, // Min value
        .96f, // Max value
        0.02f  // Step
    );
    Map<String, Param> mergerParams = new HashMap<>();
    mergerParams.put(StrategyMerger.STOP_LOSS_PERCENT, stopLossParam);
    strategyMerger.configure(mergerParams);
    strategyMerger.getSubStrategies().clear();
    strategyProvider.init(ticker, startDate, endDate, timeFrame);

    TiltFromBaseStrategy shortStrategy = (TiltFromBaseStrategy) strategyProvider
        .getStrategy("TiltFromBaseStrategy");
    List<Param> optParamListShort =
        buildOptParamsTilt(ticker, timeFrame, caseId,
            7, 10, 1,
            -1f, 1f, 0.1f,
            -1f, 1f, 0.1f);
    shortStrategy.configure(getOptParamsAsMap(optParamListShort));
    strategyMerger.addStrategy(shortStrategy, timeFrame);
    strategyMerger.init(ticker, timeFrame, startDate, endDate);

    TiltFromBaseStrategy longStrategy = (TiltFromBaseStrategy) strategyProvider
        .getStrategy("TiltFromBaseStrategy");

    List<Param> optParamListLong =
        buildOptParamsTilt(ticker, timeFrame, caseId, 24.0, 36, 4,
            0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f);

    longStrategy.configure(getOptParamsAsMap(optParamListLong));
    //strategyMerger.addStrategy(longStrategy);

    //genericOptimizator.init(strategyMerger);
    Map<Strategy, Map<String, Param>> strategyToParams =
        genericOptimizator.findOptimalParametersForAllStrategies();
    List<Param> optParamListToDB = new ArrayList<>();
    for (Map.Entry<Strategy, Map<String, Param>> entry : strategyToParams.entrySet()) {
      Strategy strategy = entry.getKey();
      Map<String, Param> optParams = entry.getValue();
      for (Map.Entry<String, Param> nameToParamEntry : optParams.entrySet()) {
        Param optParamToStore = nameToParamEntry.getValue();
        optParamToStore.setValue(
            strategyToParams.get(strategy).get(nameToParamEntry.getKey()).getValue());
        optParamToStore.setValueString(
            strategyToParams.get(strategy).get(nameToParamEntry.getKey()).getValueString());
        optParamToStore.getId().setStrategy(strategy.getClass().getSimpleName());
        optParamToStore.getId().setCaseId(caseId);
        optParamListToDB.add(optParamToStore);
      }
    }
    optParamListToDB.add(
        new Param(ticker, START_DATE, strategyMerger.getClass().getSimpleName(), caseId,
            timeFrame, strategyMerger.getClass().getSimpleName(),
            0d, strategyMerger.getStartDate().toString(), 1f, 0f, 1f)
    );
    optParamListToDB.add(
        new Param(ticker, END_DATE, strategyMerger.getClass().getSimpleName(), caseId,
            timeFrame, strategyMerger.getClass().getSimpleName(),
            0d, strategyMerger.getEndDate().toString(), 1f, 0f, 1f)
    );
    optParamDao.saveAll(optParamListToDB);
    show(genericOptimizator.getBestOverallResult(), Map.of(), Map.of(),
        strategyMerger.getStrategyName());
    return strategyToParams;
  }

  private static List<Param> buildOptParamsTilt(String ticker, TimeFrame timeFrame,
                                                String caseId,
                                                double lengthStart, float lengthMax,
                                                float lengthStep,
                                                double tiltBuyStart, float tiltBuyMax,
                                                float tiltBuyStep,
                                                double tiltSellStart, float tiltSellMax,
                                                float tiltSellStep) {
    List<Param> optParamListLong = List.of(
        new Param(ticker, LENGTH, "TiltFromBaseStrategy", caseId, timeFrame,
            "TiltFromBaseStrategy",
            lengthStart, "", (float) lengthStart, lengthMax, lengthStep),

        new Param(ticker, TILT_BUY, "TiltFromBaseStrategy", caseId, timeFrame,
            "TiltFromBaseStrategy",
            tiltBuyStart, "", (float) tiltBuyStart, tiltBuyMax, tiltBuyStep),

        new Param(ticker, TILT_SELL, "TiltFromBaseStrategy", caseId, timeFrame,
            "TiltFromBaseStrategy",
            tiltSellStart, "", (float) tiltSellStart, tiltSellMax, tiltSellStep)
    );
    return optParamListLong;
  }

  private void futurePriceCalc(String ticker, Map<String, Double> optParams) {
    double priceToBuy = futurePriceByTiltCalculator.calculatePriceBinary(ticker, TimeFrame.DAY,
        optParams.get(LENGTH).intValue(), optParams.get(TILT_BUY));
    //System.out.println("Price to buy: " + priceToBuy);
    //priceToBuy = futurePriceByTiltCalculator.calculatePrice(ticker, TimeFrame.DAY,
    //    optParams.get(OptimizatorTilt.LENGTH).intValue(), optParams.get(OptimizatorTilt.TILT_BUY));
    // System.out.println("Old Price to buy: " + priceToBuy);

    double priceToSell = futurePriceByTiltCalculator.calculatePriceBinary(ticker, TimeFrame.DAY,
        optParams.get(LENGTH).intValue(), optParams.get(TILT_SELL));
    //System.out.println("Price to sell: " + priceToSell);
    optParams.put(PRICE_TO_BUY, priceToBuy);
    optParams.put(PRICE_TO_SELL, priceToSell);
  }

  private StrategyResult evaluateStrategy(Strategy strategy) {
    buyAndHoldStrategy.init(strategy.getTicker(), strategy.getTimeFrame(),
        strategy.getStartDate(), strategy.getEndDate());
    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdownLong(buyAndHoldStrategy, strategy.getTicker(),
            strategy.getStartDate(),
            strategy.getEndDate(),
            strategy.getTimeFrame());
    StrategyResult strategyResult =
        tradeService.calculateProfitAndDrawdownLong(strategy);
    StrategyResult strategyResultShort =
        tradeService.calculateProfitAndDrawdownShort(strategy);
    strategyResult.setBuyAndHoldPnL(buyAndHoldstrategyResult.getLongPnL());
    strategyResult.setShortPnL(strategyResultShort.getShortPnL());

    System.out.println(strategy.getStrategyName() + " result: " + strategyResult);
    // System.out.println(
    //    buyAndHoldStrategy.getStrategyName() + " result: " + buyAndHoldstrategyResult);

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

    return strategyResult;
  }

  private TiltFromBaseStrategy initStrategy(TiltFromBaseStrategy tiltStrategy, TimeFrame timeFrame,
                                            String ticker,
                                            LocalDateTime startDate,
                                            LocalDateTime endDate
  ) {
    tiltStrategy.init(ticker, timeFrame, startDate, endDate);
    //{Length=44.0, TiltBuy=0.01, TiltSell=-0.05}
    //tiltStrategy.setLength(9);
    //tiltStrategy.setLength(params.get(OptimizatorTilt.LENGTH).intValue());
    //tiltStrategy.setTiltBuy(0.02);
    //tiltStrategy.setTiltBuy(params.get(OptimizatorTilt.TILT_BUY));
    //tiltStrategy.setTiltSell(-0.02);
    //tiltStrategy.setTiltSell(params.get(OptimizatorTilt.TILT_SELL));
    tiltStrategy.calcSignals();
    return tiltStrategy;
  }

  public String buildFinvizUrl(String ticker) {
    return String.format("https://finviz.com/quote.ashx?t=%s&ta=1&p=d&ty=ea", ticker.toUpperCase());
  }

  public void readOptParamsGenericAndShow(String ticker,
                                          LocalDateTime startDate,
                                          LocalDateTime endDate,
                                          TimeFrame timeFrame,
                                          String caseId) {
    Map<String, List<Param>> strategyToParams =
        optParamDao.findByTickerAndCaseIdGroupedByStrategy(ticker, caseId);
    strategyMerger.getSubStrategies().clear();
    strategyProvider.init(ticker, startDate, endDate, timeFrame);
    for (Map.Entry<String, List<Param>> entiesStrategyToParams : strategyToParams.entrySet()) {
      String strategyClass = entiesStrategyToParams.getValue().getFirst().getStrategyClass();
      if (!strategyClass.equals(strategyMerger.getClass().getSimpleName())) {
        Strategy strategy = strategyProvider.getStrategy(strategyClass);
        strategy.configure(getOptParamsAsMap(entiesStrategyToParams.getValue()));
        strategyMerger.addStrategy(strategy,
            entiesStrategyToParams.getValue().getFirst().getTimeframe());
      } else {
        strategyMerger.configure(getOptParamsAsMap(entiesStrategyToParams.getValue()));
      }
    }
    strategyMerger.init(ticker, timeFrame, startDate, endDate);
    strategyMerger.mergeSignals();
    StrategyResult strategyResult = evaluateStrategy(strategyMerger);
    show(strategyResult, Map.of(), Map.of(), strategyMerger.getStrategyName());
  }
}
