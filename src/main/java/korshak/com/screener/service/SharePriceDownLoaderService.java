package korshak.com.screener.service;

import java.time.LocalDate;

public interface SharePriceDownLoaderService {
  int fetchAndSaveData(String timeSeriesLabel, String ticker, String interval, String yearMonth);

  int fetchAndSaveData(String ticker, String yearMonth);
   int fetchAndSaveDataFromDate(String ticker, LocalDate startDate);
  String getDbTicker();
}
