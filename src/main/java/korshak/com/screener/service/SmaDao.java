package korshak.com.screener.service;

import java.time.LocalDateTime;
import java.util.List;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.TimeFrame;

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
