package korshak.com.screener;

import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.ChartService;
import korshak.com.screener.service.PriceAggregationService;
import korshak.com.screener.service.SharePriceDownLoaderService;
import korshak.com.screener.service.SmaCalculationService;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.serviceImpl.chart.ChartServiceImpl;
import korshak.com.screener.serviceImpl.TiltStrategy;
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
  @Qualifier("BuyAndHoldStrategy")
  private Strategy buyAndHoldStrategy;

  @Override
  public void run(String... args) throws Exception {
    evaluateStrategy();
    //downloadSeries();
    //aggregate();
    //calcSMA();
   // System.exit(0);
  }

  private void evaluateStrategy() {
    String ticker = "SPY";
    TimeFrame timeFrame = TimeFrame.DAY;
    StrategyResult buyAndHoldstrategyResult=tradeService.calculateProfitAndDrawdown(buyAndHoldStrategy,ticker, timeFrame);
    StrategyResult strategyResultTilt=tradeService.calculateProfitAndDrawdown(tiltStrategy,ticker, timeFrame);
    System.out.println(tiltStrategy.getName()+" result: "  +strategyResultTilt);
    System.out.println(buyAndHoldStrategy.getName()+" result: "  +buyAndHoldstrategyResult);
    System.setProperty("java.awt.headless", "false");
    ChartService chartService  = new ChartServiceImpl(tiltStrategy.getName());
    chartService.drawChart(strategyResultTilt.getPrices(),strategyResultTilt.getSignals(),((TiltStrategy)tiltStrategy).getSmaList());
    //chartService.drawChart(strategyResult.getPrices(),strategyResult.getTrades());
  }

  private void aggregate() {
    String ticker = "GLD";
    priceAggregationService.aggregateData(ticker,TimeFrame.DAY);
    System.exit(0);
  }

  private void calcSMA() {
    String ticker = "SPY";
    int startLength = 9;
    int endLength = 9;
    long start = System.currentTimeMillis();
    System.out.println("started ");
    for (int length=startLength;length<=endLength;length += 3) {
      smaCalculationService.calculateSMA(ticker, length, TimeFrame.DAY);
      System.out.println("length = " + length);
    }
    System.out.println("total = "+(System.currentTimeMillis()-start));
    System.exit(0);
  }


  private void downloadSeries() {
    final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
    final String ticker = "GLD";
    String interval = "5min";
    String year = "2022-";
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
          .fetchAndSaveData(timeSeriesLabel, ticker, interval, yearMonth);
      System.out.println("saved = " + saved);
    }
    System.exit(0);
  }
}
