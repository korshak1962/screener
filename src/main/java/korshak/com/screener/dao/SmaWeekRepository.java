package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmaWeekRepository extends JpaRepository<SmaWeek, SmaKey>, SmaRepository {
  void deleteByIdTickerAndIdLength(String ticker, int length);

  // Find all SMAs for a specific ticker and length, ordered by date
  List<SmaWeek> findByIdTickerAndIdLengthOrderByIdDateAsc(String ticker, int length);

  // Find SMAs ordered by date within a date range
  List<SmaWeek> findByIdTickerAndIdLengthAndIdDateBetweenOrderByIdDateAsc(
      String ticker,
      int length,
      LocalDateTime startDate,
      LocalDateTime endDate
  );
}
