package korshak.com.screener;

import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.SharePriceDownLoaderService;
import korshak.com.screener.service.SmaCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Override
  public void run(String... args) throws Exception {
    downloadSeries();
    //long start = System.currentTimeMillis();
    /*
    System.out.println("started ");
    calcSMA();
    System.out.println("total = "+(System.currentTimeMillis()-start));

     */
    System.exit(0);
  }

  private void calcSMA() {
    String ticker = "SPY";
    int length = 3;
    smaCalculationService.calculateSMA(ticker,length,TimeFrame.HOUR);
  }


  private void downloadSeries() {
    final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
    final String ticker = "SPY";
    String interval = "5min";
    String year = "2021-";
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
  }
}
