package korshak.com.screener.service;

public interface SharePriceDownLoaderService {
  int fetchAndSaveData(String timeSeriesLabel, String ticker, String interval, String yearMonth);

  int fetchAndSaveData(String ticker, String yearMonth);

  String getDbTicker();
}
