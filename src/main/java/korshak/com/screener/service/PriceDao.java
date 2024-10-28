package korshak.com.screener.service;

import java.time.LocalDateTime;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.TimeFrame;

public interface PriceDao {
  List<? extends BasePrice> findByDateRange(
      String ticker,
      LocalDateTime startDate,
      LocalDateTime endDate,
      TimeFrame timeFrame
  );
  List<? extends BasePrice> findAllByTicker(String ticker, TimeFrame timeFrame);
}
