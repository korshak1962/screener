package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;

interface SmaRepository {
  void deleteByIdTickerAndIdLength(String ticker, int length);
  // Find all SMAs for a specific ticker and length, ordered by date
  List<? extends BaseSma> findByIdTickerAndIdLengthOrderByIdDateAsc(String ticker, int length);
  // Find SMAs ordered by date within a date range
  List<? extends BaseSma> findByIdTickerAndIdLengthAndIdDateBetweenOrderByIdDateAsc(
      String ticker,
      int length,
      LocalDateTime startDate,
      LocalDateTime endDate
  );
}
