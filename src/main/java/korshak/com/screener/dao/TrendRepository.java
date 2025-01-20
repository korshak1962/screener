package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrendRepository extends JpaRepository<Trend, TrendKey> {
  List<Trend> findByIdTickerAndIdTimeframeAndIdDateBetweenOrderByIdDateAsc(
      String ticker,
      TimeFrame timeframe,
      LocalDateTime startDate,
      LocalDateTime endDate
  );

  void deleteByIdTickerAndIdTimeframeAndIdDateBetween(
      String ticker,
      TimeFrame timeframe,
      LocalDateTime startDate,
      LocalDateTime endDate
  );
}
