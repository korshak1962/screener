package korshak.com.screener;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.ChartService;
import korshak.com.screener.service.PriceAggregationService;
import korshak.com.screener.service.RsiService;
import korshak.com.screener.service.SharePriceDownLoaderService;
import korshak.com.screener.service.SmaCalculationService;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.service.TrendService;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.serviceImpl.AlphaVintageDownloader;
import korshak.com.screener.serviceImpl.FuturePriceByTiltCalculator;
import korshak.com.screener.serviceImpl.MoexSharePriceDownLoaderServiceImpl;
import korshak.com.screener.serviceImpl.chart.ChartServiceImpl;
import korshak.com.screener.serviceImpl.strategy.BuyAndHoldStrategyMinusDownTrend;
import korshak.com.screener.serviceImpl.strategy.BuyCloseHigherPrevClose;
import korshak.com.screener.serviceImpl.strategy.BuyHigherPrevHigh;
import korshak.com.screener.serviceImpl.strategy.DoubleTiltStrategy;
import korshak.com.screener.serviceImpl.strategy.Optimizator;
import korshak.com.screener.serviceImpl.strategy.OptimizatorDoubleTilt;
import korshak.com.screener.serviceImpl.strategy.OptimizatorTilt;
import korshak.com.screener.serviceImpl.strategy.StopLossLessThanPrevMinExtremumStrategy;
import korshak.com.screener.serviceImpl.strategy.StopLossPercentStrategy;
import korshak.com.screener.serviceImpl.strategy.StrategyMerger;
import korshak.com.screener.serviceImpl.strategy.TiltCombinedStrategy;
import korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy;
import korshak.com.screener.serviceImpl.strategy.TiltStrategy;
import korshak.com.screener.utils.ExcelExportService;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.StrategyResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("korshak.com.screener.dao")  // adjust to your package
@EnableJpaRepositories("korshak.com.screener.dao")  // adjust to your package
public class ScreenerApplication implements CommandLineRunner {

  public static void main(String[] args) {
    SpringApplication.run(ScreenerApplication.class, args);
  }

  @Autowired
  @Qualifier("AlphaVintageDownloader")
  private AlphaVintageDownloader alfaVintageDownloader;
  @Autowired
  private SmaCalculationService smaCalculationService;
  @Autowired
  private PriceAggregationService priceAggregationService;
  @Autowired
  private TradeService tradeService;
  @Autowired
  @Qualifier("TiltStrategy")
  private TiltStrategy tiltStrategy;
  @Autowired
  @Qualifier("DoubleTiltStrategy")
  private DoubleTiltStrategy doubleTiltStrategy;
  @Autowired
  @Qualifier("CombinedStrategy")
  private TiltCombinedStrategy tiltCombinedStrategy;
  @Autowired
  @Qualifier("TiltFromBaseStrategy")
  private TiltFromBaseStrategy tiltFromBaseStrategy;
  @Autowired
  @Qualifier("StopLossPercentStrategy")
  StopLossPercentStrategy stopLossPercentStrategy;
  @Autowired
  @Qualifier("StopLossLessThanPrevMinExtremumStrategy")
  StopLossLessThanPrevMinExtremumStrategy stopLossLessThanPrevMinExtremumStrategy;
  @Autowired
  @Qualifier("BuyHigherPrevHigh")
  private BuyHigherPrevHigh buyHigherPrevHigh;
  @Autowired
  @Qualifier("BuyCloseHigherPrevClose")
  private BuyCloseHigherPrevClose buyCloseHigherPrevClose;
  @Autowired
  @Qualifier("StrategyMerger")
  private StrategyMerger strategyMerger;
  @Autowired
  @Qualifier("BuyAndHoldStrategyMinusDownTrend")
  private BuyAndHoldStrategyMinusDownTrend buyAndHoldStrategyMinusDownTrend;
  @Autowired
  @Qualifier("BuyAndHoldStrategy")
  private Strategy buyAndHoldStrategy;

  @Autowired
  @Qualifier("OptimizatorDoubleTilt")
  private OptimizatorDoubleTilt optimizatorDoubleTilt;

  @Autowired
  @Qualifier("OptimizatorTilt")
  private OptimizatorTilt optimizatorTilt;

  @Autowired
  private TrendService trendService;
  @Autowired
  private FuturePriceByTiltCalculator futurePriceByTiltCalculator;

  @Autowired
  @Qualifier("moexDownloader")
  private MoexSharePriceDownLoaderServiceImpl moexDownloader;

  @Autowired
  RsiService rsiService;

  @Override
  public void run(String... args) throws Exception {
     /* optAndShow("IEMG",
        LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0),
        LocalDateTime.of(2025, Month.MARCH, 1, 0, 0));
      */
    //downloadSeries("TMOS", "2022-", 1, 12, moexDownloader);
    downloadSeries("NVTK", "2025-", 1, 12, moexDownloader);
    //downloadSeries("YY", "2022-", 1, 12, alfaVintageDownloader);
    //downloadSeries("TQQQ", "2024-", 1, 12);
    //downloadSeriesUnsafe("QQQ", "2025-", 2, 2);
    //priceAggregationService.aggregateAllTickers();
    //priceAggregationService.aggregateAllTimeFrames("NVTK_MOEX");
    //priceAggregationService.aggregateData("SPY", TimeFrame.DAY);
     //calcSMA_incremental("NVTK_MOEX",2,100);
   // calcSMA("NVTK_MOEX", 2, 50);
    //calcSMA( 2, 50);
    //trendService.calculateAndStorePriceTrend("SPXL",TimeFrame.DAY);
    //calcRSI(3,50);
    //calcRSI("SBER", 11, 50);
    System.exit(0);
  }

  private void optAndShow(String ticker,LocalDateTime startDate,LocalDateTime endDate) throws IOException {;
    TimeFrame timeFrame = TimeFrame.DAY;
    int minLength = 3;
    int maxLength = 10;
    int stepLength = 1;
    double minTiltBuy = -0.01;
    double maxTiltBuy = 0.02;
    double tiltBuyStep = 0.01;
    double minTiltSell = -0.05;
    double maxTiltSell = 0.01;
    double tiltSellStep = 0.01;
    double minStopLossPercent = .95;
    double maxStopLossPercent = .99;
    double stepOfStopLoss = 0.01;

    optimizatorTilt.configure(minLength, maxLength, stepLength, minTiltBuy, maxTiltBuy, tiltBuyStep,
        minTiltSell, maxTiltSell, tiltSellStep);
    Map<String, Double> optParams =
        optimazeStrategy(optimizatorTilt, ticker, timeFrame, startDate, endDate,
            minStopLossPercent,
            maxStopLossPercent, stepOfStopLoss);

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
    futurePriceCalc(ticker, optParams);
    evaluateStrategy(strategyMerger);
  }

  private void futurePriceCalc(String ticker, Map<String, Double> optParams) {
    double priceToBuy = futurePriceByTiltCalculator.calculatePrice(ticker, TimeFrame.DAY,
        optParams.get(OptimizatorTilt.LENGTH).intValue(), optParams.get(OptimizatorTilt.TILT_BUY));
    double priceToSell = futurePriceByTiltCalculator.calculatePrice(ticker, TimeFrame.DAY,
        optParams.get(OptimizatorTilt.LENGTH).intValue(), optParams.get(OptimizatorTilt.TILT_SELL));
    System.out.println("Price to buy: " + priceToBuy);
    System.out.println("Price to sell: " + priceToSell);
  }

  private TiltStrategy initStrategy(TiltStrategy tiltStrategy, TimeFrame timeFrame, String ticker,
                                    LocalDateTime startDate,
                                    LocalDateTime endDate) {
    tiltStrategy.init(ticker, timeFrame, startDate, endDate);
    tiltStrategy.setLength(9);
    tiltStrategy.setTiltBuy(0.02);
    tiltStrategy.setTiltSell(-0.02);
    return tiltStrategy;
  }

  private TiltCombinedStrategy initStrategy(TiltCombinedStrategy tiltStrategy, TimeFrame timeFrame,
                                            String ticker,
                                            LocalDateTime startDate,
                                            LocalDateTime endDate) {
    tiltStrategy.init(ticker, timeFrame, startDate, endDate);
    tiltStrategy.setLength(9);
    tiltStrategy.setTiltBuy(0.02);
    tiltStrategy.setTiltSell(-0.02);
    tiltStrategy.calcSignals();
    return tiltStrategy;
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

  private DoubleTiltStrategy initStrategy(DoubleTiltStrategy doubleTiltStrategy) {
    return doubleTiltStrategy;
  }

  private void evaluateStrategy(Strategy strategy) throws IOException {
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
    System.setProperty("java.awt.headless", "false");
    ChartService chartService = new ChartServiceImpl(strategy.getStrategyName());
    chartService.drawChart(strategyResultTilt.getPrices(), strategyResultTilt.getSignals()
        , strategy.getPriceIndicators()
        , strategyResultTilt.getTradesLong(), strategy.getIndicators());
    pause();
    /*chartService.drawChart(strategyResultTilt.getPrices(), strategyResultTilt.getSignalsLong()
        , ((TiltStrategy) tiltStrategy).getSmaList()
        , strategyResultTilt.getTradesLong());
     */
    //chartService.drawChart(strategyResult.getPrices(),strategyResult.getSignalsLong());
  }

  private static void pause() {
    try {
      System.out.println("Press any key to continue...");
      System.in.read();
    } catch (IOException e) {
      e.printStackTrace();
    }
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

  private void evaluateDoubleTiltStrategyMinusDownTrend() throws IOException {
    String ticker = "SPY";
    TimeFrame timeFrame = TimeFrame.WEEK;
    LocalDateTime startDate = LocalDateTime.of(2018, Month.MAY, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2024, Month.DECEMBER, 1, 0, 0);

    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdownLong(buyAndHoldStrategy, ticker,
            startDate,
            endDate,
            timeFrame);
    //StrategyResult strategyResultDoubleTilt =
    //    tradeService.calculateProfitAndDrawdownLong(tiltStrategy, ticker, timeFrame);
    buyAndHoldStrategyMinusDownTrend.init(ticker, timeFrame, startDate, endDate);

    //buyAndHoldStrategyMinusDownTrend.setTiltPeriod(5);
    buyAndHoldStrategyMinusDownTrend.setSmaLength(9);
    buyAndHoldStrategyMinusDownTrend.setTrendLengthSma(36);

    buyAndHoldStrategyMinusDownTrend.setTiltLongOpen(.02);
    buyAndHoldStrategyMinusDownTrend.setTiltLongClose(-.02);
    // added for TLT
    buyAndHoldStrategyMinusDownTrend.setTiltShortClose(-.01);
    buyAndHoldStrategyMinusDownTrend.setTiltShortOpen(-.25);
    buyAndHoldStrategyMinusDownTrend.setTiltHigherTrendLong(-.0);
    buyAndHoldStrategyMinusDownTrend.setTiltHigherTrendShort(-.2);

    StrategyResult strategyResultDoubleTiltMinusDownTrendLong =
        tradeService.calculateProfitAndDrawdownLong(buyAndHoldStrategyMinusDownTrend, ticker,
            startDate,
            endDate,
            timeFrame);
    StrategyResult strategyResultDoubleTiltShort =
        tradeService.calculateProfitAndDrawdownShort(buyAndHoldStrategyMinusDownTrend);
    System.out.println(buyAndHoldStrategyMinusDownTrend.getStrategyName() + " Long result: " +
        strategyResultDoubleTiltMinusDownTrendLong);
    System.out.println(buyAndHoldStrategyMinusDownTrend.getStrategyName() + " Short result: " +
        strategyResultDoubleTiltShort);
    System.out.println(
        buyAndHoldStrategyMinusDownTrend.getStrategyName() + " result: " +
            buyAndHoldstrategyResult);
    System.setProperty("java.awt.headless", "false");
    ChartService chartService =
        new ChartServiceImpl(buyAndHoldStrategyMinusDownTrend.getStrategyName());


    Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators = new HashMap<>();

    priceIndicators.put(
        "SMA_" + buyAndHoldStrategyMinusDownTrend.getSmaLongList().getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(buyAndHoldStrategyMinusDownTrend.getSmaLongList()));
    priceIndicators.put(
        "SMA_" + buyAndHoldStrategyMinusDownTrend.getSmaShortList().getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(buyAndHoldStrategyMinusDownTrend.getSmaShortList()));

    ExcelExportService.exportTradesToExcel(
        strategyResultDoubleTiltMinusDownTrendLong.getTradesLong(), "trades_long.xlsx");
    ExcelExportService.exportTradesToExcel(strategyResultDoubleTiltShort.getTradesShort(),
        "trades_short.xlsx");


    // Map<String, NavigableMap<LocalDateTime, Double>> indicators = strategyResultDoubleTilt.getIndicators();
    Map<String, NavigableMap<LocalDateTime, Double>> indicators = new TreeMap<>();
    //   indicators.put("shortSmaTilt",((DoubleTiltStrategy) doubleTiltStrategy).getShortSmaTiltAsMap());
    indicators.put("trendSmaTilt",
        ((DoubleTiltStrategy) buyAndHoldStrategyMinusDownTrend).getTrendSmaTiltAsMap());

    //((DoubleTiltStrategy) doubleTiltStrategy).getShortSmaTilt()
    chartService.drawChart(strategyResultDoubleTiltMinusDownTrendLong.getPrices(),
        strategyResultDoubleTiltMinusDownTrendLong.getSignals()
        , priceIndicators
        , strategyResultDoubleTiltMinusDownTrendLong.getTradesLong(), indicators);
    /*
    chartService.drawChart(strategyResultDoubleTiltShort.getPrices(), strategyResultDoubleTiltShort.getSignals()
        , priceIndicators
        , strategyResultDoubleTiltShort.getTradesShort(), indicators);

     */
  }

  private void evaluateDoubleTiltStrategy() throws IOException {
    String ticker = "SPY";
    TimeFrame timeFrame = TimeFrame.DAY;
    LocalDateTime startDate = LocalDateTime.of(2020, Month.MAY, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2024, Month.DECEMBER, 1, 0, 0);

    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdownLong(buyAndHoldStrategy, ticker,
            startDate,
            endDate,
            timeFrame);
    //StrategyResult strategyResultDoubleTilt =
    //    tradeService.calculateProfitAndDrawdownLong(tiltStrategy, ticker, timeFrame);
    doubleTiltStrategy.init(ticker, timeFrame, startDate, endDate);
    DoubleTiltStrategy fullDoubleTiltStrategy = (DoubleTiltStrategy) doubleTiltStrategy;
    //===========================================
    // fullDoubleTiltStrategy.setTiltPeriod(5);
    fullDoubleTiltStrategy.setSmaLength(9);
    fullDoubleTiltStrategy.setTrendLengthSma(36);

    fullDoubleTiltStrategy.setTiltLongOpen(.02);
    fullDoubleTiltStrategy.setTiltLongClose(-.02);
    // added for TLT
    fullDoubleTiltStrategy.setTiltShortClose(-.01);
    fullDoubleTiltStrategy.setTiltShortOpen(-.22);
    fullDoubleTiltStrategy.setTiltHigherTrendLong(-.1);
    fullDoubleTiltStrategy.setTiltHigherTrendShort(-.2);

    //==================================

    StrategyResult strategyResultDoubleTiltLong =
        tradeService.calculateProfitAndDrawdownLong(doubleTiltStrategy, ticker,
            startDate,
            endDate,
            timeFrame);
    StrategyResult strategyResultDoubleTiltShort =
        tradeService.calculateProfitAndDrawdownShort(doubleTiltStrategy);
    System.out.println(
        doubleTiltStrategy.getStrategyName() + " Long result: " + strategyResultDoubleTiltLong);
    System.out.println(
        doubleTiltStrategy.getStrategyName() + " Short result: " + strategyResultDoubleTiltShort);
    System.out.println(
        buyAndHoldStrategy.getStrategyName() + " result: " + buyAndHoldstrategyResult);
    System.setProperty("java.awt.headless", "false");
    ChartService chartService = new ChartServiceImpl(doubleTiltStrategy.getStrategyName());


    Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators = new HashMap<>();

    priceIndicators.put(
        "SMA_" + fullDoubleTiltStrategy.getSmaLongList().getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(fullDoubleTiltStrategy.getSmaLongList()));
    priceIndicators.put(
        "SMA_" + fullDoubleTiltStrategy.getSmaShortList().getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(fullDoubleTiltStrategy.getSmaShortList()));

    ExcelExportService.exportTradesToExcel(strategyResultDoubleTiltLong.getTradesLong(),
        "trades_long.xlsx");
    ExcelExportService.exportTradesToExcel(strategyResultDoubleTiltShort.getTradesShort(),
        "trades_short.xlsx");


    // Map<String, NavigableMap<LocalDateTime, Double>> indicators = strategyResultDoubleTilt.getIndicators();
    Map<String, NavigableMap<LocalDateTime, Double>> indicators = new TreeMap<>();
    //   indicators.put("shortSmaTilt",((DoubleTiltStrategy) doubleTiltStrategy).getShortSmaTiltAsMap());
    indicators.put("trendSmaTilt",
        ((DoubleTiltStrategy) doubleTiltStrategy).getTrendSmaTiltAsMap());

    //((DoubleTiltStrategy) doubleTiltStrategy).getShortSmaTilt()
    chartService.drawChart(strategyResultDoubleTiltLong.getPrices(),
        strategyResultDoubleTiltLong.getSignals()
        , priceIndicators
        , strategyResultDoubleTiltLong.getTradesLong(), indicators);
    /*
    chartService.drawChart(strategyResultDoubleTiltShort.getPrices(), strategyResultDoubleTiltShort.getSignals()
        , priceIndicators
        , strategyResultDoubleTiltShort.getTradesShort(), indicators);

     */
    pause();
  }

  private void calcSMA_incremental(String ticker, int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      smaCalculationService.calculateIncrementalSMAForAllTimeFrames(ticker, length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  private void calcSMA(String ticker, int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      smaCalculationService.calculateSMAForAllTimeFrame(ticker, length);
      System.out.println("length = " + length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  private void calcRSI(String ticker, int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      rsiService.calculateRsiForAllTimeFrames(ticker, length);
      System.out.println("length = " + length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  private void calcSMA(int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      smaCalculationService.calculateSMAForAllTimeFrameAndTickers(length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }
  private void calcRSI(int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      rsiService.calculateIncrementalRsiForAllTickersAndTimeFrames(length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }


  private void downloadSeries(final String ticker, String year, int startMonth, int finalMonth,
                              SharePriceDownLoaderService sharePriceDownLoaderService) {
    // final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
    // String interval = "5min";
    int lengthMin = 2;
    int lengthMax = 50;
    int saved = 0;
    String yearMonth;
    for (int month = startMonth; month < finalMonth + 1; month++) {
      if (month < 10) {
        yearMonth = year + "0" + month;
      } else {
        yearMonth = year + month;
      }

      System.out.println("yearMonth = " + yearMonth);
      saved += sharePriceDownLoaderService
          .fetchAndSaveData(ticker, yearMonth);
      System.out.println("saved = " + saved);
    }
    if (saved > 0) {
      priceAggregationService.aggregateAllTimeFrames(sharePriceDownLoaderService.getDbTicker());
      for (int i = lengthMin; i <= lengthMax; i++) {
        smaCalculationService.calculateIncrementalSMAForAllTimeFrames(sharePriceDownLoaderService.getDbTicker(), i);
      }
    }
    System.exit(0);
  }

  private void downloadSeriesUnsafe(final String ticker, String year, int startMonth,
                                    int finalMonth) {
    final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
    final String interval = "5min";
    int lengthMin = 2;
    int lengthMax = 50;
    int saved = 0;
    String yearMonth;
    for (int month = startMonth; month < finalMonth + 1; month++) {
      if (month < 10) {
        yearMonth = year + "0" + month;
      } else {
        yearMonth = year + month;
      }

      System.out.println("yearMonth = " + yearMonth);
      saved += alfaVintageDownloader
          .fetchAndSaveData(timeSeriesLabel, ticker, interval, yearMonth);
      System.out.println("saved = " + saved);
    }
    if (saved > 0) {
      priceAggregationService.aggregateAllTimeFrames(ticker);
      for (int i = lengthMin; i <= lengthMax; i++) {
        smaCalculationService.calculateIncrementalSMAForAllTimeFrames(ticker, i);
      }
    }
    System.exit(0);
  }

}
