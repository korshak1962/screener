package korshak.com.screener.serviceImpl.download;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import korshak.com.screener.service.download.SharePriceDownLoaderService;
import korshak.com.screener.serviceImpl.calc.Calculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class Downloader {

  private static final int LENGTH_MIN = 2;
  private static final int LENGTH_MAX = 50;
  public static final String ALPHA_VINTAGE_DOWNLOADER = "AlphaVintageDownloader";
  public static final String YAHOO_DOWNLOADER = "yahooDownloader";
  public static final String MOEX_DOWNLOADER = "moexDownloader";
  private AlphaVintageDownloader alphaVintageDownloader;
  private SharePriceDownLoaderService yahooDownloader;
  private MoexSharePriceDownLoaderServiceImpl moexDownloader;
  @Autowired
  Calculator calculator;

  public Map<String, SharePriceDownLoaderService> nameToDownloadService = new HashMap<>();

  public Downloader(
      @Qualifier(ALPHA_VINTAGE_DOWNLOADER) AlphaVintageDownloader alphaVintageDownloader,
      @Qualifier(YAHOO_DOWNLOADER) SharePriceDownLoaderService yahooDownloader,
      @Qualifier(MOEX_DOWNLOADER) MoexSharePriceDownLoaderServiceImpl moexDownloader) {
    this.alphaVintageDownloader = alphaVintageDownloader;
    this.yahooDownloader = yahooDownloader;
    this.moexDownloader = moexDownloader;
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
      saved = sharePriceDownLoaderService.downloadFromToTomorrow(ticker, startDate);
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
}
