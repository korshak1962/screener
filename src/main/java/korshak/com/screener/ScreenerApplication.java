package korshak.com.screener;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.PriceAggregationService;
import korshak.com.screener.service.RsiService;
import korshak.com.screener.service.SharePriceDownLoaderService;
import korshak.com.screener.service.SmaCalculationService;
import korshak.com.screener.service.TrendService;
import korshak.com.screener.serviceImpl.AlphaVintageDownloader;
import korshak.com.screener.serviceImpl.FuturePriceByTiltCalculator;
import korshak.com.screener.serviceImpl.MoexSharePriceDownLoaderServiceImpl;
import korshak.com.screener.serviceImpl.Reporter;
import korshak.com.screener.serviceImpl.strategy.StrategyMerger;
import korshak.com.screener.utils.ExcelExportService;
import korshak.com.screener.utils.Portfolios;
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
  @Qualifier("StrategyMerger")
  private StrategyMerger strategyMerger;
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
    LocalDateTime startDate = LocalDateTime.of(2018, Month.JANUARY, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
    //TMOS LKOH SBER MGNT  TINKOFF
    List<String> strategyNames = List.of("TiltStrategy");
    //reporter.readParamsGetStrategyResult("QQQ", startDate, endDate, TimeFrame.DAY,strategyNames);
    //reporter.findResultFor2strategies("QQQ", startDate, endDate, TimeFrame.DAY);
   // for (String ticker : Portfolios.NAME_TO_TICKERS.get(Portfolios.US)){
   //   trendService.calculateAndStorePriceTrendForAllTimeframes(ticker);
   // }
    reporter.findOptParamAndSaveGeneric("SPY", startDate, endDate, TimeFrame.DAY, "only1");
    //reporter.readOptParamsGenericAndShow("SPY", startDate, endDate, TimeFrame.DAY,"only1");
    //reporter.findResultFor2strategies("QQQ", startDate, endDate, TimeFrame.WEEK, TimeFrame.DAY);
    //reporter.opt("QQQ", startDate, endDate, TimeFrame.DAY);

    LocalDateTime startDateEval = LocalDateTime.of(2025, Month.JANUARY, 1, 0, 0);
    LocalDateTime endDateEval = LocalDateTime.of(2025, Month.MAY, 1, 0, 0);
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
