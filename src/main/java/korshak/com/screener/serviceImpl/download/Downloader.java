package korshak.com.screener.serviceImpl.download;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.download.SharePriceDownLoaderService;
import korshak.com.screener.serviceImpl.calc.Calculator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class Downloader {

  private static final int LENGTH_MIN = 2;
  private static final int LENGTH_MAX = 50;
  public static final String ALPHA_VINTAGE_DOWNLOADER = "AlphaVintageDownloader";
  public static final String YAHOO_DOWNLOADER = "yahooDownloader";
  public static final String MOEX_DOWNLOADER = "moexDownloader";
  private final AlphaVintageDownloader alphaVintageDownloader;
  private final SharePriceDownLoaderService yahooDownloader;
  private final MoexSharePriceDownLoaderServiceImpl moexDownloader;
  private final Calculator calculator;
  private final PriceDao priceDao;

  public Map<String, SharePriceDownLoaderService> nameToDownloadService = new HashMap<>();

  public Downloader(
      @Qualifier(ALPHA_VINTAGE_DOWNLOADER) AlphaVintageDownloader alphaVintageDownloader,
      @Qualifier(YAHOO_DOWNLOADER) SharePriceDownLoaderService yahooDownloader,
      @Qualifier(MOEX_DOWNLOADER) MoexSharePriceDownLoaderServiceImpl moexDownloader,
      Calculator calculator,
      PriceDao priceDao) {
    this.alphaVintageDownloader = alphaVintageDownloader;
    this.yahooDownloader = yahooDownloader;
    this.moexDownloader = moexDownloader;
    this.calculator = calculator;
    this.priceDao = priceDao;
    nameToDownloadService.put(ALPHA_VINTAGE_DOWNLOADER, this.alphaVintageDownloader);
    //https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=Tencent&apikey=2NYM2EF6HJZUCXAL
    //test https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=IBM&interval=5min&month=2024-01&outputsize=full&apikey=2NYM2EF6HJZUCXAL
    //https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=TCEHY&outputsize=full&apikey=YOUR_API_KEY
    nameToDownloadService.put(YAHOO_DOWNLOADER, this.yahooDownloader);
    nameToDownloadService.put(MOEX_DOWNLOADER, this.moexDownloader);
  }

  private void downloadSeries(final List<String> tickers, final String startDate,
                              SharePriceDownLoaderService sharePriceDownLoaderService) {
    for (String ticker : tickers) {
      downloadSeries(ticker, startDate, sharePriceDownLoaderService);
    }
  }

  private void downloadSeries(final String ticker, final String startDate,
                              SharePriceDownLoaderService sharePriceDownLoaderService) {
    int saved =
        sharePriceDownLoaderService.fetchAndSaveDataFromDate(ticker, LocalDate.parse(startDate));
    System.out.println("saved = " + saved);
    if (saved > 0) {
      calculator.agregateAndSmaCalc(LENGTH_MIN, LENGTH_MAX,
          sharePriceDownLoaderService.getDbTicker());
    }
    //System.exit(0);
  }

  public void downloadSeriesFromToTomorrow(final List<String> tickers, LocalDate startDate,
                                            SharePriceDownLoaderService sharePriceDownLoaderService) {
    int saved = 0;
    for (String ticker : tickers) {
      saved = sharePriceDownLoaderService.downloadFromDateUpToday(ticker, startDate);
      if (saved > 0) {
        calculator.agregateAndSmaCalc(LENGTH_MIN, LENGTH_MAX,
            sharePriceDownLoaderService.getDbTicker());
      }
    }
  }

  public void downloadSeries(final List<String> tickers,
                             int startYear, int startMonth,
                             int endYear, int finalMonth,
                             SharePriceDownLoaderService sharePriceDownLoaderService) {
    for (String ticker : tickers) {
      downloadSeries(ticker, startYear, startMonth, endYear, finalMonth,
          sharePriceDownLoaderService);
    }
  }

  private void downloadSeries(final String ticker, int startYear, int startMonth, int endYear,
                              int endMonth,
                              SharePriceDownLoaderService sharePriceDownLoaderService) {
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
      calculator.agregateAndSmaCalc(LENGTH_MIN, LENGTH_MAX,
          sharePriceDownLoaderService.getDbTicker());
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
    int saved = 0;

    for (int month = startMonth; month < finalMonth + 1; month++) {

      System.out.println("yearMonth = " + year + " " + month);
      saved += alphaVintageDownloader
          .fetchAndSaveData(timeSeriesLabel, ticker, interval, month, year);
      System.out.println("saved = " + saved);
    }
    if (saved > 0) {
      calculator.agregateAndSmaCalc(LENGTH_MIN, LENGTH_MAX, alphaVintageDownloader.getDbTicker());
    }
    System.exit(0);
  }






  /**
   * Downloads data for a single ticker starting from the last available date in the database
   * to the current date if there is a gap.
   *
   * @param ticker The ticker symbol to update
   * @param downloader The downloader service to use
   * @return Number of records updated
   */
  public int updateTickerUpToday(final String ticker, SharePriceDownLoaderService downloader) {
    // Check if we need to append _MOEX suffix for MOEX downloader
    String dbTicker = ticker;
    if (downloader == moexDownloader && !ticker.endsWith("_MOEX")) {
      dbTicker = ticker + "_MOEX";
    }

    // Get the latest date from the database for this ticker using DAY timeframe
    List<? extends BasePrice> latestPrices = priceDao.findAllByTicker(dbTicker, TimeFrame.DAY);

    if (latestPrices.isEmpty()) {
      System.out.println("No existing data for " + ticker);
      return 0;
    }

    // Sort prices by date in descending order and get the latest date
    BasePrice latestPrice = latestPrices.getLast();
    LocalDateTime latestDateTime = latestPrice.getId().getDate();
    LocalDate latestDate = latestDateTime.toLocalDate();
    LocalDate today = LocalDate.now();

    // Check if the data is already up to date (within 1 day)
    if (latestDate.isEqual(today) ) {
      System.out.println("Data for " + ticker + " is already up to date (latest: " + latestDate + ")");
      return 0;
    }
    // Download data from the day after the latest date to today
    LocalDate startDate = latestDate.plusDays(1);
    System.out.println("Updating " + ticker + " from " + startDate + " to " + today);

    int saved = downloader.downloadFromDateUpToday(ticker, startDate);

    if (saved > 0) {
      calculator.agregateAndSmaCalc(LENGTH_MIN, LENGTH_MAX, downloader.getDbTicker());
      System.out.println("Updated " + saved + " records for " + ticker);
    } else {
      System.out.println("No new data available for " + ticker);
    }
    return saved;
  }

  /**
   * Downloads data for a list of tickers starting from the last available date in the database
   * to the current date if there is a gap.
   *
   * @param tickers List of ticker symbols to update
   * @param downloader The downloader service to use
   * @return Map of tickers to number of records updated
   */
  public Map<String, Integer> updateTickersUpToday(
      final List<String> tickers,
      SharePriceDownLoaderService downloader) {

    Map<String, Integer> tickerToRecordsUpdated = new HashMap<>();

    for (String ticker : tickers) {
      int recordsUpdated = updateTickerUpToday(ticker, downloader);
      tickerToRecordsUpdated.put(ticker, recordsUpdated);
    }
    return tickerToRecordsUpdated;
  }
}
