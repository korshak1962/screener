package korshak.com.screener;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
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
import korshak.com.screener.serviceImpl.Reporter;
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
import korshak.com.screener.utils.Portfolios;
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

  @Autowired
  @Qualifier("StopLossPercentStrategy")
  StopLossPercentStrategy stopLossPercentStrategy;
  @Autowired
  @Qualifier("StopLossLessThanPrevMinExtremumStrategy")
  StopLossLessThanPrevMinExtremumStrategy stopLossLessThanPrevMinExtremumStrategy;
  @Autowired
  RsiService rsiService;
  @Autowired
  Reporter reporter;
  @Autowired
  @Qualifier("AlphaVintageDownloader")
  private AlphaVintageDownloader alfaVintageDownloader;
  @Autowired
  @Qualifier("yahooDownloader")
  private SharePriceDownLoaderService yahooDownloader;
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

  public static void main(String[] args) {
    SpringApplication.run(ScreenerApplication.class, args);
  }

  static void pause() {
    try {
      System.out.println("Press any key to continue...");
      System.in.read();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run(String... args) throws Exception {
    LocalDateTime startDate = LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
    String ticker = "SNGS_MOEX";
   // reporter.optAndShow(ticker, startDate, endDate, TimeFrame.DAY);

    LocalDateTime startDateEval = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
    LocalDateTime endDateEval = LocalDateTime.of(2025, Month.MARCH, 1, 0, 0);
    //reporter.readAndShow(ticker, startDateEval, endDateEval, TimeFrame.DAY);
    reporter.createExcelReport(Utils.addSuffix(Portfolios.NAME_TO_TICKERS.get(Portfolios.MOEX),
        "_MOEX"),startDateEval, endDateEval, TimeFrame.DAY, Portfolios.MOEX);
    //reporter.createExcelReport(tickers, startDateEval, endDateEval, TimeFrame.DAY);


    //downloadSeries(Portfolios.NAME_TO_TICKERS.get(Portfolios.US_WATCH), "2025-", 1, 2, yahooDownloader);
    //downloadSeriesFromToTomorrow(Portfolios.NAME_TO_TICKERS.get(Portfolios.US), LocalDate.now().minusDays(7), yahooDownloader);
     //reporter.optAndShow(ticker, startDate, endDate, TimeFrame.DAY);

    //downloadSeries("NVIDIA", "2024-", 1, 12,alfaVintageDownloader);
    //downloadSeriesUnsafe("MOMO", "2024-", 1, 12);
    //downloadSeries(Portfolios.NAME_TO_TICKERS.get(Portfolios.US), "2025-", 2, 2, yahooDownloader);
    //downloadSeries("QQQ", "2025-01-01", yahooDownloader);
//    downloadSeries("SNGS", "2018-01-01", moexDownloader);
    //downloadSeries(Portfolios.NAME_TO_TICKERS.get(Portfolios.MOEX), "2025-", 2, 2, moexDownloader);
    //downloadSeries(Portfolios.NAME_TO_TICKERS.get(Portfolios.MOEX), "2025-", 2, 2, moexDownloader);
    //downloadSeriesUnsafe("VALE", "2025-", 2, 2);

    //calcSMA("Т_MOEX",2,50);
    //calcSMA(Portfolios.NAME_TO_TICKERS.get(Portfolios.US_WATCH), 2, 50);
    //downloadSeries("T ", "2025-", 1, 2, alfaVintageDownloader);
    //downloadSeries("TQQQ", "2024-", 1, 12);
    //downloadSeriesUnsafe("TLT", "2025-", 1, 2);
    //priceAggregationService.aggregateAllTickers();
    //priceAggregationService.aggregateAllTimeFrames("NVTK_MOEX");
    // priceAggregationService.aggregateData("TMOS_MOEX", TimeFrame.DAY);
    //calcSMA_incremental("NVTK_MOEX",2,100);
    //calcSMA( 2, 50);
    //trendService.calculateAndStorePriceTrendForAllTimeframes("SPXL");
    //calcRSI(3,50);
    //calcRSI("SBER", 11, 50);
    System.exit(0);
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
        buyAndHoldStrategyMinusDownTrend.getTrendSmaTiltAsMap());

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
    DoubleTiltStrategy fullDoubleTiltStrategy = doubleTiltStrategy;
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
        doubleTiltStrategy.getTrendSmaTiltAsMap());

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

  private void calcSMA(List<String> tickers, int startLength, int endLength){
    for (String ticker : tickers){
      calcSMA(ticker,startLength,endLength);
    }
  }

  private void calcSMA(String ticker, int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      smaCalculationService.calculateSMAForAllTimeFrame(ticker, length);
      System.out.println("length = " + length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    //System.exit(0);
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

  private void downloadSeries(final List<String> tickers, final String startDate,
                              SharePriceDownLoaderService sharePriceDownLoaderService){
    for (String ticker : tickers) {
      downloadSeries(ticker, startDate, sharePriceDownLoaderService);
    }
  }

  private void downloadSeries(final String ticker, final String startDate,
                              SharePriceDownLoaderService sharePriceDownLoaderService) {
    int lengthMin = 2;
    int lengthMax = 50;
    int saved =
        sharePriceDownLoaderService.fetchAndSaveDataFromDate(ticker, LocalDate.parse(startDate));
    System.out.println("saved = " + saved);
    if (saved > 0) {
      priceAggregationService.aggregateAllTimeFrames(sharePriceDownLoaderService.getDbTicker());
      for (int i = lengthMin; i <= lengthMax; i++) {
        smaCalculationService.calculateIncrementalSMAForAllTimeFrames(
            sharePriceDownLoaderService.getDbTicker(),
            i);
      }
    }
    //System.exit(0);
  }

  private void downloadSeriesFromToTomorrow(final List<String> tickers, LocalDate startDate,
                                            SharePriceDownLoaderService sharePriceDownLoaderService) {
    int lengthMin = 2;
    int lengthMax = 50;
    int saved = 0;
    for (String ticker : tickers) {
      saved = sharePriceDownLoaderService.downloadFromToTomorrow(ticker, startDate);
      if (saved > 0) {
        priceAggregationService.aggregateAllTimeFrames(sharePriceDownLoaderService.getDbTicker());
        for (int i = lengthMin; i <= lengthMax; i++) {
          smaCalculationService.calculateIncrementalSMAForAllTimeFrames(
              sharePriceDownLoaderService.getDbTicker(), i);
        }
      }
    }
  }

  private void downloadSeries(final List<String> tickers, String year, int startMonth,
                              int finalMonth,
                              SharePriceDownLoaderService sharePriceDownLoaderService) {
    for (String ticker : tickers) {
      downloadSeries(ticker, year, startMonth, finalMonth, sharePriceDownLoaderService);
    }
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
        smaCalculationService.calculateIncrementalSMAForAllTimeFrames(
            sharePriceDownLoaderService.getDbTicker(), i);
      }
    }
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
