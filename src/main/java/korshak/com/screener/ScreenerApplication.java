package korshak.com.screener;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.ChartService;
import korshak.com.screener.service.PriceAggregationService;
import korshak.com.screener.service.SharePriceDownLoaderService;
import korshak.com.screener.service.SmaCalculationService;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.serviceImpl.strategy.DoubleTiltStrategy;
import korshak.com.screener.serviceImpl.strategy.BuyAndHoldStrategyMinusDownTrend;
import korshak.com.screener.serviceImpl.Optimizator;
import korshak.com.screener.serviceImpl.strategy.TiltStrategy;
import korshak.com.screener.serviceImpl.chart.ChartServiceImpl;
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
  private SharePriceDownLoaderService sharePriceDownLoaderService;
  @Autowired
  private SmaCalculationService smaCalculationService;
  @Autowired
  private PriceAggregationService priceAggregationService;
  @Autowired
  private TradeService tradeService;
  @Autowired
  @Qualifier("TiltStrategy")
  private Strategy tiltStrategy;
  @Autowired
  @Qualifier("DoubleTiltStrategy")
  private DoubleTiltStrategy doubleTiltStrategy;
  @Autowired
  @Qualifier("BuyAndHoldStrategyMinusDownTrend")
  private BuyAndHoldStrategyMinusDownTrend buyAndHoldStrategyMinusDownTrend;
  @Autowired
  @Qualifier("BuyAndHoldStrategy")
  private Strategy buyAndHoldStrategy;

  @Override
  public void run(String... args) throws Exception {
    //optimazeDoubleTiltStrategy();
    //evaluateDoubleTiltStrategy();
    //   evaluateDoubleTiltStrategyMinusDownTrend();
    //evaluateStrategy();
    downloadSeries();
    //priceAggregationService.aggregateAllTickers();
    //priceAggregationService.aggregateAllTimeFrames("VALE");
    //calcSMA(1,100);
     System.exit(0);
  }

  private void evaluateStrategy() throws IOException {
    String ticker = "SPY";
    TimeFrame timeFrame = TimeFrame.DAY;
    LocalDateTime startDate = LocalDateTime.of(2024, Month.JANUARY,1,0,0);
    LocalDateTime endDate = LocalDateTime.of(2025, Month.JANUARY,1,0,0);

    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdownLong(buyAndHoldStrategy, ticker,
            startDate,
            endDate,
            timeFrame);
    //StrategyResult strategyResultTilt =
    //    tradeService.calculateProfitAndDrawdownLong(tiltStrategy, ticker, timeFrame);

    StrategyResult strategyResultTilt =
        tradeService.calculateProfitAndDrawdownLong(tiltStrategy, ticker,
             startDate,
             endDate,
            timeFrame);
    System.out.println(tiltStrategy.getName() + " result: " + strategyResultTilt);
    System.out.println(tiltStrategy.getName() + " result: " + buyAndHoldstrategyResult);
    System.setProperty("java.awt.headless", "false");
    ChartService chartService = new ChartServiceImpl(tiltStrategy.getName());
    chartService.drawChart(strategyResultTilt.getPrices(), strategyResultTilt.getSignals()
        , tiltStrategy.getPriceIndicators()
        , strategyResultTilt.getTradesLong(), tiltStrategy.getIndicators());
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

  @Autowired
  Optimizator optimizator;
  private void optimazeDoubleTiltStrategy(){
    String ticker = "SPY";
    TimeFrame timeFrame = TimeFrame.WEEK;
    LocalDateTime startDate = LocalDateTime.of(2021, Month.JANUARY,1,0,0);
    LocalDateTime endDate = LocalDateTime.of(2024, Month.DECEMBER,1,0,0);

    optimizator.init(ticker,timeFrame,startDate,endDate);
    Map<String, Double> params = optimizator.findOptimumParameters();
    System.out.println(params);
    System.exit(0);
  }

  private void evaluateDoubleTiltStrategyMinusDownTrend() throws IOException {
    String ticker = "SPY";
    TimeFrame timeFrame = TimeFrame.WEEK;
    LocalDateTime startDate = LocalDateTime.of(2018, Month.MAY,1,0,0);
    LocalDateTime endDate = LocalDateTime.of(2024, Month.DECEMBER,1,0,0);

    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdownLong(buyAndHoldStrategy, ticker,
            startDate,
            endDate,
            timeFrame);
    //StrategyResult strategyResultDoubleTilt =
    //    tradeService.calculateProfitAndDrawdownLong(tiltStrategy, ticker, timeFrame);
    buyAndHoldStrategyMinusDownTrend.init(ticker,timeFrame,startDate, endDate);

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
    System.out.println(buyAndHoldStrategyMinusDownTrend.getName() + " Long result: " + strategyResultDoubleTiltMinusDownTrendLong);
    System.out.println(buyAndHoldStrategyMinusDownTrend.getName() + " Short result: " + strategyResultDoubleTiltShort);
    System.out.println(buyAndHoldStrategyMinusDownTrend.getName() + " result: " + buyAndHoldstrategyResult);
    System.setProperty("java.awt.headless", "false");
    ChartService chartService = new ChartServiceImpl(buyAndHoldStrategyMinusDownTrend.getName());


    Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators = new HashMap<>();

    priceIndicators.put("SMA_" + buyAndHoldStrategyMinusDownTrend.getSmaLongList().getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(buyAndHoldStrategyMinusDownTrend.getSmaLongList()));
    priceIndicators.put("SMA_" + buyAndHoldStrategyMinusDownTrend.getSmaShortList().getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(buyAndHoldStrategyMinusDownTrend.getSmaShortList()));

    ExcelExportService.exportTradesToExcel(strategyResultDoubleTiltMinusDownTrendLong.getTradesLong(), "trades_long.xlsx");
    ExcelExportService.exportTradesToExcel(strategyResultDoubleTiltShort.getTradesShort(), "trades_short.xlsx");


    // Map<String, NavigableMap<LocalDateTime, Double>> indicators = strategyResultDoubleTilt.getIndicators();
    Map<String, NavigableMap<LocalDateTime, Double>> indicators = new TreeMap<>();
    //   indicators.put("shortSmaTilt",((DoubleTiltStrategy) doubleTiltStrategy).getShortSmaTiltAsMap());
    indicators.put("trendSmaTilt",((DoubleTiltStrategy) buyAndHoldStrategyMinusDownTrend).getTrendSmaTiltAsMap());

    //((DoubleTiltStrategy) doubleTiltStrategy).getShortSmaTilt()
    chartService.drawChart(strategyResultDoubleTiltMinusDownTrendLong.getPrices(), strategyResultDoubleTiltMinusDownTrendLong.getSignals()
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
    LocalDateTime startDate = LocalDateTime.of(2020, Month.MAY,1,0,0);
    LocalDateTime endDate = LocalDateTime.of(2024, Month.DECEMBER,1,0,0);

    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdownLong(buyAndHoldStrategy, ticker,
            startDate,
            endDate,
            timeFrame);
    //StrategyResult strategyResultDoubleTilt =
    //    tradeService.calculateProfitAndDrawdownLong(tiltStrategy, ticker, timeFrame);
    doubleTiltStrategy.init(ticker,timeFrame,startDate, endDate);
    DoubleTiltStrategy fullDoubleTiltStrategy = (DoubleTiltStrategy)doubleTiltStrategy;
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
    System.out.println(doubleTiltStrategy.getName() + " Long result: " + strategyResultDoubleTiltLong);
    System.out.println(doubleTiltStrategy.getName() + " Short result: " + strategyResultDoubleTiltShort);
    System.out.println(buyAndHoldStrategy.getName() + " result: " + buyAndHoldstrategyResult);
    System.setProperty("java.awt.headless", "false");
    ChartService chartService = new ChartServiceImpl(doubleTiltStrategy.getName());


    Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators = new HashMap<>();

    priceIndicators.put("SMA_" + fullDoubleTiltStrategy.getSmaLongList().getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(fullDoubleTiltStrategy.getSmaLongList()));
    priceIndicators.put("SMA_" + fullDoubleTiltStrategy.getSmaShortList().getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(fullDoubleTiltStrategy.getSmaShortList()));

    ExcelExportService.exportTradesToExcel(strategyResultDoubleTiltLong.getTradesLong(), "trades_long.xlsx");
    ExcelExportService.exportTradesToExcel(strategyResultDoubleTiltShort.getTradesShort(), "trades_short.xlsx");


    // Map<String, NavigableMap<LocalDateTime, Double>> indicators = strategyResultDoubleTilt.getIndicators();
    Map<String, NavigableMap<LocalDateTime, Double>> indicators = new TreeMap<>();
 //   indicators.put("shortSmaTilt",((DoubleTiltStrategy) doubleTiltStrategy).getShortSmaTiltAsMap());
    indicators.put("trendSmaTilt",((DoubleTiltStrategy) doubleTiltStrategy).getTrendSmaTiltAsMap());

    //((DoubleTiltStrategy) doubleTiltStrategy).getShortSmaTilt()
    chartService.drawChart(strategyResultDoubleTiltLong.getPrices(), strategyResultDoubleTiltLong.getSignals()
        , priceIndicators
        , strategyResultDoubleTiltLong.getTradesLong(), indicators);
    /*
    chartService.drawChart(strategyResultDoubleTiltShort.getPrices(), strategyResultDoubleTiltShort.getSignals()
        , priceIndicators
        , strategyResultDoubleTiltShort.getTradesShort(), indicators);

     */
    pause();
  }

  private void calcSMA(int startLength,int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      smaCalculationService.calculateSMAForAllTimeFrameAndTickers(length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start)/60000);
    System.exit(0);
  }


  private void downloadSeries() {
   // final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
   // String interval = "5min";
    final String ticker = "T";
    String year = "2024-";
    String yearMonth;
    int startMonth = 1;
    int finalMonth = 12;
    for (int month = startMonth; month < finalMonth + 1; month++) {
      if (month < 10) {
        yearMonth = year + "0" + month;
      } else {
        yearMonth = year + month;
      }

      System.out.println("yearMonth = " + yearMonth);
      int saved = sharePriceDownLoaderService
          .fetchAndSaveData( ticker, yearMonth);
      System.out.println("saved = " + saved);
    }

    priceAggregationService.aggregateAllTimeFrames(ticker);
    //calcSMA(ticker,TimeFrame.MONTH, 1,50);
    System.exit(0);
  }
}
