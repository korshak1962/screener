package korshak.com.screener;

import static korshak.com.screener.serviceImpl.download.Downloader.ALPHA_VINTAGE_DOWNLOADER;
import static korshak.com.screener.serviceImpl.download.Downloader.MOEX_DOWNLOADER;
import static korshak.com.screener.serviceImpl.download.Downloader.YAHOO_DOWNLOADER;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.calc.TrendService;
import korshak.com.screener.serviceImpl.download.Downloader;
import korshak.com.screener.serviceImpl.report.Reporter;
import korshak.com.screener.utils.Portfolios;
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
  @Autowired
  Reporter reporter;
  @Autowired
  Downloader downloader;
  @Autowired
  TrendService trendService;


  public static void main(String[] args) {
    SpringApplication.run(ScreenerApplication.class, args);
  }

  @Override
  public void run(String... args) {

    //downloadAll();

    LocalDateTime startDate = LocalDateTime.of(2018, Month.JANUARY, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2025, Month.MAY, 1, 0, 0);

    //reporter.readParamsGetStrategyResult("QQQ", startDate, endDate, TimeFrame.DAY,strategyNames);
    //reporter.findResultFor2strategies("QQQ", startDate, endDate, TimeFrame.DAY);
   // for (String ticker : Portfolios.US){
   //   trendService.calculateAndStorePriceTrendForAllTimeframes(ticker);
   // }
    //reporter.findOptParamAndSaveGeneric("SPY", startDate, endDate, TimeFrame.DAY, "only111");
    reporter.readOptParamsGenericAndShow("SPY", startDate, endDate, TimeFrame.DAY,"only111");
    //reporter.findResultFor2strategies("QQQ", startDate, endDate, TimeFrame.WEEK, TimeFrame.DAY);
    //reporter.opt("QQQ", startDate, endDate, TimeFrame.DAY);

    LocalDateTime startDateEval = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
    LocalDateTime endDateEval = LocalDateTime.of(2025, Month.MAY, 1, 0, 0);
    String ticker = "SBER_MOEX";
    //reporter.createExcelReport(List.of(ticker), startDateEval, endDateEval, TimeFrame.DAY,ticker);
   // reporter.evaluateAndShow(buyCloseHigherPrevClose, ticker, startDateEval, endDateEval,
     //   TimeFrame.WEEK);

    Map<TimeFrame, List<String>> timeFrameToStrategyNames = new HashMap<>();
/*    List<String> strategyNames = List.of("TrendChangeStrategy");
    timeFrameToStrategyNames.put(TimeFrame.DAY, strategyNames);
    List<String> strategyNamesDay = List.of("TrendChangeStrategy");
    timeFrameToStrategyNames.put(TimeFrame.WEEK, strategyNamesDay);

 */
    //reporter.readAndShow(ticker, startDateEval, endDateEval, TimeFrame.DAY);
    // reporter.createExcelReport(Utils.addSuffix(Portfolios.NAME_TO_TICKERS.get(Portfolios.MOEX),
    //     "_MOEX"), startDateEval, endDateEval, TimeFrame.DAY, Portfolios.MOEX);
    //   reporter.createExcelReport(Utils.addSuffix(Portfolios.NAME_TO_TICKERS.get(Portfolios.US),
    //  ""),startDateEval, endDateEval, TimeFrame.DAY, Portfolios.US);
    System.exit(0);
  }

  private void downloadAll() {

    downloader.downloadSeries(Portfolios.INDEXES,
        2007, 1,
        2025, 5,
        downloader.nameToDownloadService.get(ALPHA_VINTAGE_DOWNLOADER));

    downloader.updateTickersUpToday(Portfolios.INDEXES,
        downloader.nameToDownloadService.get(YAHOO_DOWNLOADER));
    for (String ticker : Portfolios.INDEXES){
      trendService.calculateAndStorePriceTrendForAllTimeframes(ticker);
    }

    downloader.updateTickersUpToday(Portfolios.US_WATCH,
        downloader.nameToDownloadService.get(YAHOO_DOWNLOADER));
    for (String ticker : Portfolios.US_WATCH){
      trendService.calculateAndStorePriceTrendForAllTimeframes(ticker);
    }

    downloader.updateTickersUpToday(Portfolios.US_SECTOR_ETF,
        downloader.nameToDownloadService.get(YAHOO_DOWNLOADER));
    for (String ticker : Portfolios.US_SECTOR_ETF){
      trendService.calculateAndStorePriceTrendForAllTimeframes(ticker);
    }

    downloader.updateTickersUpToday(Portfolios.CHINA,
        downloader.nameToDownloadService.get(YAHOO_DOWNLOADER));
    for (String ticker : Portfolios.CHINA){
      trendService.calculateAndStorePriceTrendForAllTimeframes(ticker);
    }

    downloader.updateTickersUpToday(Portfolios.ALL,
        downloader.nameToDownloadService.get(YAHOO_DOWNLOADER));
    for (String ticker : Portfolios.ALL){
      trendService.calculateAndStorePriceTrendForAllTimeframes(ticker);
    }

    downloader.downloadSeries(Portfolios.MOEX,
        2025, 4,
        2025, 5,
        downloader.nameToDownloadService.get(MOEX_DOWNLOADER));
    for (String ticker : Portfolios.MOEX){
      trendService.calculateAndStorePriceTrendForAllTimeframes(ticker);
    }
  }

}
