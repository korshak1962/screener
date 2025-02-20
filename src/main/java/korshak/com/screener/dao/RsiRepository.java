package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;

public interface RsiRepository {
  void deleteByIdTickerAndIdLength(String ticker, int length);

  List<? extends BaseRsi> findByIdTickerAndIdLengthOrderByIdDateAsc(String ticker, int length);

  List<? extends BaseRsi> findByIdTickerAndIdLengthAndIdDateBetweenOrderByIdDateAsc(
      String ticker,
      int length,
      LocalDateTime startDate,
      LocalDateTime endDate
  );
}

