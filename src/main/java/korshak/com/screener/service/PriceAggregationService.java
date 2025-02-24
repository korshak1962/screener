package korshak.com.screener.service;

import java.time.LocalTime;
import korshak.com.screener.dao.TimeFrame;

public interface PriceAggregationService {
  public static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);  // ET
  public static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0); // ET

  void aggregateData(String ticker, TimeFrame timeFrame);

  void aggregateAllTimeFrames(String ticker);

  void aggregateAllTickers();
}