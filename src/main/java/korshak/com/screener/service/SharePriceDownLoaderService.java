package korshak.com.screener.service;

public interface SharePriceDownLoaderService {
  //  SharePrice createSharePrice(SharePrice sharePrice);

 //   Page<SharePrice> getSharePricesBetweenDates(String ticker, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

  //  SharePrice updateSharePrice(SharePrice sharePrice);

  //  void deleteSharePrice(String ticker, LocalDateTime date);

    void fetchAndSaveData(String timeSeriesLabel, String ticker, String interval, String month);
}
