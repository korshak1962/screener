package korshak.com.screener.service;

public interface SharePriceDownLoaderService {
    int fetchAndSaveData(String timeSeriesLabel, String ticker, String interval, String month);

}
