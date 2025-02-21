package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  /**
   * Finds the latest trend for a ticker and timeframe before or at a specific date
   *
   * @param ticker The ticker symbol
   * @param timeframe The timeframe
   * @param date The reference date
   * @return The latest trend before or at the reference date, or null if none exists
   */
  @Query(value = "SELECT * FROM trends t WHERE t.ticker = :ticker AND t.timeframe = :timeframe " +
      "AND t.date <= :date ORDER BY t.date DESC LIMIT 1", nativeQuery = true)
  Trend findTopByIdTickerAndIdTimeframeAndIdDateLessThanEqualOrderByIdDateDesc(
      @Param("ticker") String ticker,
      @Param("timeframe") String timeframe,
      @Param("date") LocalDateTime date
  );
}
