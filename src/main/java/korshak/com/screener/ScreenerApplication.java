package korshak.com.screener;

import static korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy.LENGTH;
import static korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy.TILT_BUY;
import static korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy.TILT_SELL;

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
  RsiService rsiService;
  @Autowired
  Reporter reporter;
  @Autowired
  @Qualifier("AlphaVintageDownloader")
  private AlphaVintageDownloader alphaVintageDownloader;
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
    LocalDateTime startDate = LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
    //TMOS LKOH SBER MGNT  TINKOFF
    List<String> strategyNames = List.of("TiltFromBaseStrategy");
    //reporter.readParamsGetStrategyResult("QQQ", startDate, endDate, TimeFrame.DAY,strategyNames);
    //reporter.findResultFor2strategies("QQQ", startDate, endDate, TimeFrame.DAY);
   // for (String ticker : Portfolios.NAME_TO_TICKERS.get(Portfolios.US)){
   //   trendService.calculateAndStorePriceTrendForAllTimeframes(ticker);
   // }
    reporter.findOptParamAndSaveGeneric("SPY", startDate, endDate, TimeFrame.DAY, "onlyShort4");
    //reporter.readOptParamsGenericAndShow("SPY", startDate, endDate, TimeFrame.DAY,"onlyShort2");
    //reporter.findResultFor2strategies("QQQ", startDate, endDate, TimeFrame.WEEK, TimeFrame.DAY);
    //reporter.opt("QQQ", startDate, endDate, TimeFrame.DAY);

    LocalDateTime startDateEval = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
    LocalDateTime endDateEval = LocalDateTime.of(2025, Month.APRIL, 1, 0, 0);
    String ticker = "SBER_MOEX";
    //reporter.createExcelReport(List.of(ticker), startDateEval, endDateEval, TimeFrame.DAY,ticker);
    //Reporter.STOP_LOSS_MAX_PERCENT = .9;
   // reporter.evaluateAndShow(buyCloseHigherPrevClose, ticker, startDateEval, endDateEval,
    //    TimeFrame.WEEK);

    Map<TimeFrame, List<String>> timeFrameToStrategyNames = new HashMap<>();
/*    List<String> strategyNames = List.of("TrendChangeStrategy");
    timeFrameToStrategyNames.put(TimeFrame.DAY, strategyNames);
    List<String> strategyNamesDay = List.of("TrendChangeStrategy");
    timeFrameToStrategyNames.put(TimeFrame.WEEK, strategyNamesDay);

 */
    timeFrameToStrategyNames.put(TimeFrame.DAY,
        List.of("TiltFromBaseStrategy"));
    //timeFrameToStrategyNames.put(TimeFrame.DAY,
    //    List.of("TiltFromBaseStrategy"));
   //reporter.readAndShow(timeFrameToStrategyNames, "QQQ",
    //     LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0),
    //     LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0));

    //reporter.readAndShow(ticker, startDateEval, endDateEval, TimeFrame.DAY);
    // reporter.createExcelReport(Utils.addSuffix(Portfolios.NAME_TO_TICKERS.get(Portfolios.MOEX),
    //     "_MOEX"), startDateEval, endDateEval, TimeFrame.DAY, Portfolios.MOEX);
    //   reporter.createExcelReport(Utils.addSuffix(Portfolios.NAME_TO_TICKERS.get(Portfolios.US),
    //  ""),startDateEval, endDateEval, TimeFrame.DAY, Portfolios.US);

  //  downloadSeries(Portfolios.NAME_TO_TICKERS.get(Portfolios.ALL),
   //     2025, 2,2025, 3, yahooDownloader);
   //  downloadSeriesFromToTomorrow(Portfolios.ALL,
   //      LocalDate.now().minusDays(8), yahooDownloader);


    // downloadSeries("AAXJ", 2024, 1,2025, 1, alphaVintageDownloader);
    //https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=Tencent&apikey=2NYM2EF6HJZUCXAL
    //test https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=IBM&interval=5min&month=2024-01&outputsize=full&apikey=2NYM2EF6HJZUCXAL
    //https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=TCEHY&outputsize=full&apikey=YOUR_API_KEY
    //downloadSeries(Portfolios.US_SECTOR_ETF, 2024, 1, 2025, 2, alphaVintageDownloader);

    //downloadSeriesUnsafe("MOMO", "2024-", 1, 12);
    //  downloadSeries(Portfolios.US_WATCH, 2025, 3,2025, 4, yahooDownloader);
    //downloadSeries("QQQ", "2025-01-01", yahooDownloader);
//    downloadSeries("SNGS", "2018-01-01", moexDownloader);
    //   downloadSeries(Portfolios.MOEX,
    //     2025, 4,2025, 4, moexDownloader);

    //downloadSeriesUnsafe("VALE", "2025-", 2, 2);

    //calcSMA(ticker,2,50);
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
        optParams.get(LENGTH).intValue(), optParams.get(TILT_BUY));
    double priceToSell = futurePriceByTiltCalculator.calculatePrice(ticker, TimeFrame.DAY,
        optParams.get(LENGTH).intValue(), optParams.get(TILT_SELL));
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
    tiltStrategy.setLength(params.get(LENGTH).intValue());
    //tiltStrategy.setTiltBuy(0.02);
    tiltStrategy.setTiltBuy(params.get(TILT_BUY));
    //tiltStrategy.setTiltSell(-0.02);
    tiltStrategy.setTiltSell(params.get(TILT_SELL));
    tiltStrategy.calcSignals();
    return tiltStrategy;
  }

  private DoubleTiltStrategy initStrategy(DoubleTiltStrategy doubleTiltStrategy) {
    return doubleTiltStrategy;
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

  private void calcSMA(List<String> tickers, int startLength, int endLength) {
    for (String ticker : tickers) {
      calcSMA(ticker, startLength, endLength);
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
                              SharePriceDownLoaderService sharePriceDownLoaderService) {
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

  private void downloadSeries(final List<String> tickers,
                              int startYear, int startMonth,
                              int endYear,int finalMonth,
                              SharePriceDownLoaderService sharePriceDownLoaderService) {
    for (String ticker : tickers) {
      downloadSeries(ticker, startYear, startMonth, endYear, finalMonth,
          sharePriceDownLoaderService);
    }
  }

  private void downloadSeries(final String ticker, int startYear, int startMonth, int endYear,
                              int endMonth,
                              SharePriceDownLoaderService sharePriceDownLoaderService) {
    // final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
    // String interval = "5min";
    int lengthMin = 2;
    int lengthMax = 50;
    int saved = 0;
    String yearMonth;
    if (startYear == endYear) {
      saved =
          downloadForGivenYear(ticker, startYear, startMonth, endMonth, sharePriceDownLoaderService,
              saved);
    } else {
      for (int year = startYear; year <= endYear; year++) {
        if (year == startYear) {
          saved = downloadForGivenYear(ticker, year, startMonth, 12, sharePriceDownLoaderService,
              saved);
        } else if (year == endYear) {
          saved =
              downloadForGivenYear(ticker, year, 1, endMonth, sharePriceDownLoaderService, saved);
        } else {
          saved = downloadForGivenYear(ticker, year, 1, 12, sharePriceDownLoaderService, saved);
        }
      }
    }
    if (saved > 0) {
      priceAggregationService.aggregateAllTimeFrames(sharePriceDownLoaderService.getDbTicker());
      for (int i = lengthMin; i <= lengthMax; i++) {
        smaCalculationService.calculateIncrementalSMAForAllTimeFrames(
            sharePriceDownLoaderService.getDbTicker(), i);
      }
    }
  }

  private static int downloadForGivenYear(String ticker, int year, int startMonth, int endMonth,
                                          SharePriceDownLoaderService sharePriceDownLoaderService,
                                          int saved) {
    for (int month = startMonth; month < endMonth + 1; month++) {

      System.out.println(ticker + " year = " + year + " month = " + month);
      saved += sharePriceDownLoaderService
          .fetchAndSaveData(ticker, year, month);
      System.out.println("saved = " + saved);
    }
    return saved;
  }

  private void downloadSeriesUnsafe(final String ticker, int year, int startMonth,
                                    int finalMonth) {
    final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
    final String interval = "5min";
    int lengthMin = 2;
    int lengthMax = 50;
    int saved = 0;

    for (int month = startMonth; month < finalMonth + 1; month++) {

      System.out.println("yearMonth = " + year + " " + month);
      saved += alphaVintageDownloader
          .fetchAndSaveData(timeSeriesLabel, ticker, interval, month,year);
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
