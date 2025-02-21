package korshak.com.screener.service;

import java.time.LocalDateTime;
import java.util.List;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.Trend;
import org.springframework.transaction.annotation.Transactional;

public interface TrendService {
  List<Trend> calculateAndStorePriceTrend(String ticker, TimeFrame timeFrame);

  Trend findLatestTrendBeforeDate(String ticker, TimeFrame timeFrame, LocalDateTime date);

  void calculateAndStorePriceTrendForAllTimeframes(String ticker,
                                                   LocalDateTime startDate, LocalDateTime endDate);

  @Transactional
  List<Trend> calculateAndStorePriceTrend(String ticker, TimeFrame timeFrame,
                                          LocalDateTime startDate, LocalDateTime endDate);
}
