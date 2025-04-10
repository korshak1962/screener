package korshak.com.screener.service.download;

import java.time.LocalDate;

public interface SharePriceDownLoaderService {
  int fetchAndSaveData(String timeSeriesLabel, String ticker, String interval, int year,int month);

  int fetchAndSaveData(String ticker, int year,int month);

  int fetchAndSaveDataFromDate(String ticker, LocalDate startDate);

  String getDbTicker();

  int downloadFromDateUpToday(String ticker, LocalDate startDate);
}
