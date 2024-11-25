package korshak.com.screener;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.ChartService;
import korshak.com.screener.service.PriceAggregationService;
import korshak.com.screener.service.SharePriceDownLoaderService;
import korshak.com.screener.service.SmaCalculationService;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.serviceImpl.DoubleTiltStrategy;
import korshak.com.screener.serviceImpl.TiltStrategy;
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
  private Strategy doubleTiltStrategy;
  @Autowired
  @Qualifier("BuyAndHoldStrategy")
  private Strategy buyAndHoldStrategy;

  @Override
  public void run(String... args) throws Exception {
    evaluateDoubleTiltStrategy();
    //evaluateStrategy();
    //downloadSeries();
    //aggregate();
    //calcSMA();
    // System.exit(0);
  }

  private void evaluateStrategy() throws IOException {
    String ticker = "SPY";
    TimeFrame timeFrame = TimeFrame.DAY;
    LocalDateTime startDate = LocalDateTime.of(2021, Month.JANUARY,1,0,0);
    LocalDateTime endDate = LocalDateTime.of(2024, Month.DECEMBER,1,0,0);

    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdown(buyAndHoldStrategy, ticker,
            startDate,
            endDate,
            timeFrame);
    //StrategyResult strategyResultTilt =
    //    tradeService.calculateProfitAndDrawdown(tiltStrategy, ticker, timeFrame);

    StrategyResult strategyResultTilt =
        tradeService.calculateProfitAndDrawdown(tiltStrategy, ticker,
             startDate,
             endDate,
            timeFrame);
    System.out.println(tiltStrategy.getName() + " result: " + strategyResultTilt);
    System.out.println(buyAndHoldStrategy.getName() + " result: " + buyAndHoldstrategyResult);
    System.setProperty("java.awt.headless", "false");
    ChartService chartService = new ChartServiceImpl(tiltStrategy.getName());
    TiltStrategy tiltStrategyFull = (TiltStrategy) tiltStrategy;
    List<? extends BaseSma> smaList = tiltStrategyFull.getSmaList();
    Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators = new HashMap<>();
    List<? extends BaseSma> trendSmaList = tiltStrategyFull.getSma(TimeFrame.DAY,45);
    priceIndicators.put("SMA_" + trendSmaList.getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(trendSmaList));
    priceIndicators.put("SMA_" + smaList.getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(smaList));
    Map<String, NavigableMap<LocalDateTime, Double>> indicators = new HashMap<>();
    indicators.put("Tilt",tiltStrategyFull.getDateToTiltValue());

      // strategyResultTilt.getIndicators();
    chartService.drawChart(strategyResultTilt.getPrices(), strategyResultTilt.getSignals()
        , priceIndicators
        , strategyResultTilt.getTradesLong(), indicators);
    /*chartService.drawChart(strategyResultTilt.getPrices(), strategyResultTilt.getSignals()
        , ((TiltStrategy) tiltStrategy).getSmaList()
        , strategyResultTilt.getTradesLong());
     */
    //chartService.drawChart(strategyResult.getPrices(),strategyResult.getSignals());
  }
  private void evaluateDoubleTiltStrategy() throws IOException {
    String ticker = "SPY";
    TimeFrame timeFrame = TimeFrame.DAY;
    LocalDateTime startDate = LocalDateTime.of(2021, Month.JANUARY,1,0,0);
    LocalDateTime endDate = LocalDateTime.of(2024, Month.DECEMBER,1,0,0);

    StrategyResult buyAndHoldstrategyResult =
        tradeService.calculateProfitAndDrawdown(buyAndHoldStrategy, ticker,
            startDate,
            endDate,
            timeFrame);
    //StrategyResult strategyResultDoubleTilt =
    //    tradeService.calculateProfitAndDrawdown(tiltStrategy, ticker, timeFrame);
    doubleTiltStrategy.init(ticker,timeFrame,startDate, endDate);
    DoubleTiltStrategy fullDoubleTiltStrategy = (DoubleTiltStrategy)doubleTiltStrategy;
    fullDoubleTiltStrategy.setTiltPeriod(5);
    fullDoubleTiltStrategy.setLongLength(45);

    fullDoubleTiltStrategy.setShortLength(9);
    fullDoubleTiltStrategy.setTiltShortBuy(.02);
    fullDoubleTiltStrategy.setTiltShortSell(-.02);
    fullDoubleTiltStrategy.setTiltLongBuy(-100);
    fullDoubleTiltStrategy.setTiltLongSell(-200);

    StrategyResult strategyResultDoubleTilt =
        tradeService.calculateProfitAndDrawdown(doubleTiltStrategy, ticker,
            startDate,
            endDate,
            timeFrame);
    System.out.println(doubleTiltStrategy.getName() + " result: " + strategyResultDoubleTilt);
    System.out.println(buyAndHoldStrategy.getName() + " result: " + buyAndHoldstrategyResult);
    System.setProperty("java.awt.headless", "false");
    ChartService chartService = new ChartServiceImpl(doubleTiltStrategy.getName());


    Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators = new HashMap<>();

    priceIndicators.put("SMA_" + fullDoubleTiltStrategy.getSmaLongList().getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(fullDoubleTiltStrategy.getSmaLongList()));
    priceIndicators.put("SMA_" + fullDoubleTiltStrategy.getSmaShortList().getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(fullDoubleTiltStrategy.getSmaShortList()));

    ExcelExportService.exportTradesToExcel(strategyResultDoubleTilt.getTradesLong(), "trades_report.xlsx");


    // strategyResultDoubleTilt.getIndicators();
    Map<String, NavigableMap<LocalDateTime, Double>> indicators =
        strategyResultDoubleTilt.getIndicators();
    //((DoubleTiltStrategy) doubleTiltStrategy).getShortSmaTilt()
    chartService.drawChart(strategyResultDoubleTilt.getPrices(), strategyResultDoubleTilt.getSignals()
        , priceIndicators
        , strategyResultDoubleTilt.getTradesLong(), indicators);
    /*chartService.drawChart(strategyResultDoubleTilt.getPrices(), strategyResultDoubleTilt.getSignals()
        , ((TiltStrategy) tiltStrategy).getSmaList()
        , strategyResultDoubleTilt.getTradesLong());
     */
    //chartService.drawChart(strategyResult.getPrices(),strategyResult.getSignals());
  }

  private void aggregate() {
    String ticker = "SPY";
    priceAggregationService.aggregateData(ticker, TimeFrame.DAY);
    System.exit(0);
  }

  private void calcSMA() {
    String ticker = "SPY";
    TimeFrame timeFrame = TimeFrame.DAY;
    int startLength = 3;
    int endLength = 201;
    long start = System.currentTimeMillis();
    System.out.println("started ");
    for (int length = startLength; length <= endLength; length += 3) {

      smaCalculationService.calculateSMA(ticker, length, timeFrame);
      System.out.println("length = " + length);
    }
    System.out.println("total = " + (System.currentTimeMillis() - start));
    System.exit(0);
  }


  private void downloadSeries() {
    final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
    final String ticker = "QQQ";
    String interval = "5min";
    String year = "2022-";
    String yearMonth;
    int startMonth = 5;
    int finalMonth = 12;
    for (int month = startMonth; month < finalMonth + 1; month++) {
      if (month < 10) {
        yearMonth = year + "0" + month;
      } else {
        yearMonth = year + month;
      }

      System.out.println("yearMonth = " + yearMonth);
      int saved = sharePriceDownLoaderService
          .fetchAndSaveData(timeSeriesLabel, ticker, interval, yearMonth);
      System.out.println("saved = " + saved);
    }
    System.exit(0);
  }
}
