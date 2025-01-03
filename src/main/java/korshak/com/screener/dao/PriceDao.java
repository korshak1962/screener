package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface PriceDao {
  List<? extends BasePrice> findByDateRange(
      String ticker,
      LocalDateTime startDate,
      LocalDateTime endDate,
      TimeFrame timeFrame
  );

  List<? extends BasePrice> findAllByTicker(String ticker, TimeFrame timeFrame);

  // Generic save method
  <T extends BasePrice> List<T> saveAll(List<T> prices);

  Set<String> findUniqueTickers();
}