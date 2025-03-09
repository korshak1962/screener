package korshak.com.screener.service;

import java.time.LocalDateTime;
import java.util.List;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.Trend;
import org.springframework.transaction.annotation.Transactional;

public interface TrendService {

  /**
   * Finds the latest trend change before the specified date.
   * A trend change is where the trend field value differs from the latest trend value.
   *
   * @param ticker      The stock ticker
   * @param timeFrame   The time frame to consider
   * @param latestTrend The cutoff trend
   * @return The latest Trend object where a change in trend occurred, or null if none found
   */
  Trend findLatestTrendChangeBefore(String ticker, TimeFrame timeFrame, Trend latestTrend);

  List<Trend> calculateAndStorePriceTrend(String ticker, TimeFrame timeFrame);

  void calculateAndStorePriceTrendForAllTimeframes(String ticker);

  Trend findLatestTrendBeforeDate(String ticker, TimeFrame timeFrame, LocalDateTime date);

  void calculateAndStorePriceTrendForAllTimeframes(String ticker,
                                                   LocalDateTime startDate, LocalDateTime endDate);

  @Transactional
  List<Trend> calculateAndStorePriceTrend(String ticker, TimeFrame timeFrame,
                                          LocalDateTime startDate, LocalDateTime endDate);

  @Transactional
  List<Trend> findByIdTickerAndIdTimeframeAndIdDateBetweenOrderByIdDateAsc(String ticker,
                                                                           TimeFrame timeframe,
                                                                           LocalDateTime startDate,
                                                                           LocalDateTime endDate);
}
