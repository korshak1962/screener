package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;

public interface SmaDao {
  void deleteByTickerAndLength(String ticker, int length, TimeFrame timeFrame);
  void saveAll(List<? extends BaseSma> smaList, TimeFrame timeFrame);
  List<? extends BaseSma> findAllByTicker(String ticker, TimeFrame timeFrame, int length);
  List<? extends BaseSma> findByDateRange(
      String ticker,
      LocalDateTime startDate,
      LocalDateTime endDate,
      TimeFrame timeFrame,
      int length
  );
}
