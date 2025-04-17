package korshak.com.screener.serviceImpl.report;

import static korshak.com.screener.serviceImpl.strategy.StrategyMerger.END_DATE;
import static korshak.com.screener.serviceImpl.strategy.StrategyMerger.START_DATE;
import static korshak.com.screener.serviceImpl.strategy.TiltStrategy.LENGTH;
import static korshak.com.screener.serviceImpl.strategy.TiltStrategy.TILT_BUY;
import static korshak.com.screener.serviceImpl.strategy.TiltStrategy.TILT_SELL;
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
import java.util.Set;
import korshak.com.screener.dao.OptParamDao;
import korshak.com.screener.dao.Param;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.Trend;
import korshak.com.screener.service.calc.TradeService;
import korshak.com.screener.service.calc.TrendService;
import korshak.com.screener.service.chart.ChartService;
import korshak.com.screener.service.strategy.Configurable;
import korshak.com.screener.service.strategy.PostTradeStrategy;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.serviceImpl.calc.FuturePriceByTiltCalculator;
import korshak.com.screener.serviceImpl.chart.ChartServiceImpl;
import korshak.com.screener.serviceImpl.strategy.BaseStrategy;
import korshak.com.screener.serviceImpl.strategy.GenericOptimizator;
import korshak.com.screener.serviceImpl.strategy.SimpleStoplossStrategy;
import korshak.com.screener.serviceImpl.strategy.StrategyMerger;
import korshak.com.screener.serviceImpl.strategy.StrategyProvider;
import korshak.com.screener.serviceImpl.strategy.TiltStrategy;
import korshak.com.screener.serviceImpl.strategy.TrendChangeStrategy;
import korshak.com.screener.utils.ExcelExportService;
import korshak.com.screener.vo.StrategyResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
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
  private final ApplicationContext applicationContext;
  private final StrategyProvider strategyProvider;
  private final GenericOptimizator genericOptimizator;
  private final StrategyMerger strategyMerger;
  private final FuturePriceByTiltCalculator futurePriceByTiltCalculator;
  private final TradeService tradeService;
  private final TiltStrategy tiltStrategy;
  private final Strategy buyAndHoldStrategy;
  private final TrendService trendService;
  private final OptParamDao optParamDao;
  private final TrendChangeStrategy trendChangeStrategy;
  public Map<String, StrategyResult> tickerToResult = new HashMap<>();

  public Reporter(ApplicationContext applicationContext, StrategyProvider strategyProvider,
                  @Qualifier("GenericOptimizator") GenericOptimizator genericOptimizator,
                  @Qualifier("StrategyMerger") StrategyMerger strategyMerger,
                  FuturePriceByTiltCalculator futurePriceByTiltCalculator,
                  TradeService tradeService,
                  @Qualifier("TiltStrategy") TiltStrategy tiltStrategy,
                  @Qualifier("BuyAndHoldStrategy") Strategy buyAndHoldStrategy,
                  TrendService trendService,
                  OptParamDao optParamDao,
                  @Qualifier("TrendChangeStrategy") TrendChangeStrategy trendChangeStrategy) {
    this.applicationContext = applicationContext;
    this.strategyProvider = strategyProvider;
    this.strategyMerger = strategyMerger;
    this.futurePriceByTiltCalculator = futurePriceByTiltCalculator;
    this.tradeService = tradeService;
    this.tiltStrategy = tiltStrategy;
    this.buyAndHoldStrategy = buyAndHoldStrategy;
    this.trendService = trendService;
    this.optParamDao = optParamDao;
    this.trendChangeStrategy = trendChangeStrategy;
    this.genericOptimizator = genericOptimizator;
  }

  private static void show(StrategyResult strategyResult,
                           String chartName) {
    System.setProperty("java.awt.headless", "false");
    ChartService chartService = new ChartServiceImpl(chartName);
    chartService.drawChart(strategyResult.getPrices(), strategyResult.getSignals()
        , strategyResult.getPriceIndicators()
        , strategyResult.getTradesLong(), strategyResult.getIndicators());
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
    strategyMerger.getPostTradeStrategies().clear();
    strategyMerger
        .addStrategies(
            subStrategies
        )
        .init(ticker, timeFrame, startDate, endDate)
        .mergeSignals();
    return evaluateStrategy(strategyMerger);
  }

  public StrategyResult getStrategyResult(BaseStrategy baseStrategy, String ticker,
                                          LocalDateTime startDate,
                                          LocalDateTime endDate, TimeFrame timeFrame) {
    strategyMerger
        .addStrategy(baseStrategy)
        .init(ticker, timeFrame, startDate, endDate)
        .mergeSignals();
    return evaluateStrategy(strategyMerger);
  }

  public StrategyResult getStrategyResult(String ticker, LocalDateTime startDate,
                                          LocalDateTime endDate, TimeFrame timeFrame) {

    BaseStrategy baseStrategy =
        initStrategy(tiltStrategy, timeFrame, ticker, startDate, endDate);
    List<Strategy> strategies = new ArrayList<>();
    strategies.add(baseStrategy);
    return getStrategyResult(ticker, startDate, endDate, timeFrame,
        STOP_LOSS_MAX_PERCENT, strategies);
  }

  public StrategyResult readParamsGetStrategyResult(String ticker, LocalDateTime startDate,
                                                    LocalDateTime endDate, TimeFrame timeFrame,
                                                    List<String> strategies) {
    strategyMerger.getSubStrategies().clear();
    strategyMerger.getPostTradeStrategies().clear();
    strategyMerger.init(ticker, timeFrame, startDate, endDate);
    strategyProvider.init(ticker, startDate, endDate);
    for (String strategyName : strategies) {
      List<Param> params = optParamDao
          .findValuesByTickerAndTimeframeAndStrategy(ticker, timeFrame, strategyName);
      addStrategyToMerger(params);
    }
    List<Param> params = optParamDao
        .findValuesByTickerAndTimeframeAndStrategy(ticker, timeFrame,
            strategyMerger.getClass().getSimpleName());

    strategyMerger.mergeSignals();
    StrategyResult strategyResult = evaluateStrategy(strategyMerger);
    show(strategyResult, strategyMerger.getStrategyName());
    System.out.println(strategyResult);
    return strategyResult;
  }

  private StrategyResult getStrategyResult(String ticker, LocalDateTime startDate,
                                           LocalDateTime endDate, TimeFrame timeFrame,
                                           double stopLossMaxPercent,
                                           List<Strategy> strategies) {

    strategyMerger
        .init(ticker, timeFrame, startDate, endDate);
    Map<String, Double> optParams = new HashMap<>();
    for (Strategy strategy : strategies) {
      strategyMerger.addStrategy(strategy);
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
    show(strategyResult, strategyMerger.getStrategyName());
    return strategyResult;
  }

  private List<Strategy> getStrategies(Map<TimeFrame, List<String>> timeFrameToStrategyNames,
                                       String ticker, LocalDateTime startDate,
                                       LocalDateTime endDate) {
    List<Strategy> strategies = new ArrayList<>();
    for (Map.Entry<TimeFrame, List<String>> entry : timeFrameToStrategyNames.entrySet()) {
      strategyProvider.init(ticker, startDate, endDate);
      for (String strategyName : entry.getValue()) {
        Strategy strategy = strategyProvider.getStrategyAndInit(strategyName, entry.getKey());
        strategies.add(strategy);
      }
    }
    return strategies;
  }

  public StrategyResult readAndShow(String ticker, LocalDateTime startDate, LocalDateTime endDate,
                                    TimeFrame timeFrame) throws IOException {
    StrategyResult strategyResult =
        readParamsGetStrategyResult(ticker, startDate, endDate, timeFrame);
    show(strategyResult, strategyMerger.getStrategyName());
    return strategyResult;
  }

  public StrategyResult evaluateAndShow(List<Strategy> subStrategies,
                                        String ticker, LocalDateTime startDate,
                                        LocalDateTime endDate,
                                        TimeFrame timeFrame) throws IOException {
    StrategyResult strategyResult =
        getStrategyResult(subStrategies, ticker, startDate, endDate, timeFrame);
    show(strategyResult, strategyMerger.getStrategyName());
    return strategyResult;
  }

  public StrategyResult evaluateAndShow(BaseStrategy baseStrategy,
                                        String ticker, LocalDateTime startDate,
                                        LocalDateTime endDate,
                                        TimeFrame timeFrame) throws IOException {
    baseStrategy.init(ticker, timeFrame, startDate, endDate);
    StrategyResult strategyResult =
        getStrategyResult(baseStrategy, ticker, startDate, endDate, timeFrame);
    show(strategyResult, strategyMerger.getStrategyName());
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

    strategyProvider.init(ticker, startDate, endDate);
    List<Strategy> subStrategies = new ArrayList<>();

    Strategy strategyHigh = strategyProvider.getStrategy(TrendChangeStrategy.class);
    //strategyHigh.configure();timeFrameHigh
    subStrategies.add(strategyHigh);
    Strategy strategyLow = strategyProvider.getStrategy(TrendChangeStrategy.class);
    //strategyLow.configure(); timeframeLow
    subStrategies.add(strategyLow);
    StrategyResult strategyResult =
        getStrategyResult(subStrategies, ticker, startDate, endDate, timeframeLow);
    show(strategyResult, strategyMerger.getStrategyName());
  }


  public Map<Configurable, Map<String, Param>> findOptParamAndSaveGeneric(String ticker,
                                                                          LocalDateTime startDate,
                                                                          LocalDateTime endDate,
                                                                          TimeFrame timeFrame,
                                                                          String caseId) {
    strategyMerger.getSubStrategies().clear();
    strategyMerger.getPostTradeStrategies().clear();
    configurePostTradeStrategies(SimpleStoplossStrategy.class, ticker, timeFrame, caseId, 0.9, 0.9f,
        .98f, 0.04f);
    strategyProvider.init(ticker, startDate, endDate);

    addTiltStrats(ticker, timeFrame, caseId);

    /*
    addStrategyToMerger(getParamsForTrendStrat(
        ticker, timeFrame, caseId, "TrendChangeStrategyShort","TrendChangeStrategy", "short"));
    addStrategyToMerger(getParamsForTrendStrat(
        ticker, TimeFrame.DAY, caseId, "TrendChangeStrategyLong","TrendChangeStrategy", "long"));
     */
    strategyMerger.init(ticker, timeFrame, startDate, endDate);

    Map<Configurable, Map<String, Param>>
        strategyToParams = findAndSaveOptParam(ticker, timeFrame, caseId);

    show(genericOptimizator.getBestOverallResult(),
        strategyMerger.getStrategyName());
    return strategyToParams;
  }

  private Map<Configurable, Map<String, Param>> findAndSaveOptParam(String ticker,
                                                                    TimeFrame timeFrame,
                                                                    String caseId) {
    Map<Configurable, Map<String, Param>> strategyToParams =
        genericOptimizator.findOptimalParametersForAllStrategies();
    List<Param> optParamListToDB = new ArrayList<>();
    for (Map.Entry<Configurable, Map<String, Param>> entry : strategyToParams.entrySet()) {
      Configurable strategy = entry.getKey();
      Map<String, Param> optParams = entry.getValue();
      for (Map.Entry<String, Param> nameToParamEntry : optParams.entrySet()) {
        Param optParamToStore = nameToParamEntry.getValue();
        optParamToStore.setValue(
            strategyToParams.get(strategy).get(nameToParamEntry.getKey()).getValue());
        optParamToStore.setValueString(
            strategyToParams.get(strategy).get(nameToParamEntry.getKey()).getValueString());
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
    return strategyToParams;
  }

  private static List<Param> getParamsForTrendStrat(String ticker, TimeFrame timeFrame,
                                                    String caseId,String strategyName,
                                                    String strategyClass, String paramName) {
    List<Param> optParamList = new ArrayList<>();
    Param param = new Param(ticker, paramName, strategyName, caseId, timeFrame,
        strategyClass,
        0, "", 0, 0, 1);
    optParamList.add(param);
    return optParamList;
  }

  private void addTiltStrats(String ticker, TimeFrame timeFrame, String caseId) {
    List<Param> paramList = buildOptParamsTilt(ticker, timeFrame, caseId,
        40, 50, 2,
        -0.1f, 0.1f, 0.1f,
        -0.1f, 0.1f, 0.1f);
    addStrategyToMerger(paramList);

    /*
    List<Param> optParamListLong =
        buildOptParamsTilt(ticker, timeFrame, caseId, 24.0, 36, 4,
            0.01f, 0.01f, 0.01f,
            0.01f, 0.01f, 0.01f);

    addStrategyToMerger(timeFrame, "TiltStrategy", optParamListLong);
     */
  }

  private void addStrategyToMerger(List<Param> optParamList) {
    String strategyClassName = optParamList.getFirst().getStrategyClass();
    if (!strategyClassName.contains(".")) {
      strategyClassName = "korshak.com.screener.serviceImpl.strategy." + strategyClassName;
    }
    try {
      if (Strategy.class.isAssignableFrom(Class.forName(strategyClassName))) {
        Strategy subStrategy =
            strategyProvider.getStrategyAndInit(strategyClassName,
                optParamList.getFirst().getTimeframe());
        subStrategy.configure(getOptParamsAsMap(optParamList));
        strategyMerger.addStrategy(subStrategy);
      }
      if (PostTradeStrategy.class.isAssignableFrom(Class.forName(strategyClassName))) {
        PostTradeStrategy postTradeStrategy = (PostTradeStrategy)applicationContext.getBean(Class.forName(strategyClassName));
        postTradeStrategy.configure(getOptParamsAsMap(optParamList));
        strategyMerger.getPostTradeStrategies().add(postTradeStrategy);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private void configurePostTradeStrategies(Class<? extends PostTradeStrategy> postTradeClass,
                                            String ticker, TimeFrame timeFrame, String caseId,
                                            double stopLoss, float minStopLoss, float maxStopLoss,
                                            float step) {
    PostTradeStrategy postTradeStrategy = applicationContext.getBean(postTradeClass);
    Param stopLossParam = new Param(
        ticker,
        SimpleStoplossStrategy.STOP_LOSS_PERCENT,
        postTradeStrategy.getClass().getSimpleName(),
        caseId,
        timeFrame,
        postTradeStrategy.getClass().getSimpleName(),
        stopLoss,  // Start value
        "",
        minStopLoss, // Min value
        maxStopLoss, // Max value
        step  // Step
    );
    Map<String, Param> mergerParams = new HashMap<>();
    mergerParams.put(SimpleStoplossStrategy.STOP_LOSS_PERCENT, stopLossParam);
    postTradeStrategy.configure(mergerParams);
    strategyMerger.getPostTradeStrategies().add(postTradeStrategy);
  }

  private static List<Param> buildOptParamsTilt(String ticker, TimeFrame timeFrame,
                                                String caseId,
                                                double lengthStart, float lengthMax,
                                                float lengthStep,
                                                double tiltBuyStart, float tiltBuyMax,
                                                float tiltBuyStep,
                                                double tiltSellStart, float tiltSellMax,
                                                float tiltSellStep) {
    String tiltStrategyClass = "TiltStrategy";
    List<Param> optParamListLong = List.of(
        new Param(ticker, LENGTH, tiltStrategyClass, caseId, timeFrame,
            tiltStrategyClass,
            lengthStart, "", (float) lengthStart, lengthMax, lengthStep),

        new Param(ticker, TILT_BUY, tiltStrategyClass, caseId, timeFrame,
            tiltStrategyClass,
            tiltBuyStart, "", (float) tiltBuyStart, tiltBuyMax, tiltBuyStep),

        new Param(ticker, TILT_SELL, tiltStrategyClass, caseId, timeFrame,
            tiltStrategyClass,
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

  private StrategyResult evaluateStrategy(StrategyMerger strategy) {
    buyAndHoldStrategy.init(strategy.getTicker(), strategy.getTimeFrame(),
        strategy.getStartDate(), strategy.getEndDate());
    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdownLong(buyAndHoldStrategy);
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

  private TiltStrategy initStrategy(TiltStrategy tiltStrategy, TimeFrame timeFrame,
                                    String ticker,
                                    LocalDateTime startDate,
                                    LocalDateTime endDate
  ) {
    tiltStrategy.init(ticker, timeFrame, startDate, endDate);
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
    strategyMerger.getSubStrategies().clear();
    strategyMerger.getPostTradeStrategies().clear();
    Map<String, List<Param>> strategyToParams =
        optParamDao.findByTickerAndCaseIdGroupedByStrategy(ticker, caseId);
    strategyProvider.init(ticker, startDate, endDate);
    for (Map.Entry<String, List<Param>> entiesStrategyToParams : strategyToParams.entrySet()) {
      addStrategyToMerger(entiesStrategyToParams.getValue());
    }
    strategyMerger.init(ticker, timeFrame, startDate, endDate);
    strategyMerger.mergeSignals();
    StrategyResult strategyResult = evaluateStrategy(strategyMerger);
    show(strategyResult, strategyMerger.getStrategyName());
  }
}
