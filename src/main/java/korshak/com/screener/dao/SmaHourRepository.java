package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface SmaHourRepository extends JpaRepository<SmaHour, SmaKey>, SmaRepository {
  void deleteByIdTickerAndIdLength(String ticker, int length);

  // Find all SMAs for a specific ticker and length, ordered by date
  List<SmaHour> findByIdTickerAndIdLengthOrderByIdDateAsc(String ticker, int length);

  // Find SMAs ordered by date within a date range
  List<SmaHour> findByIdTickerAndIdLengthAndIdDateBetweenOrderByIdDateAsc(
      String ticker,
      int length,
      LocalDateTime startDate,
      LocalDateTime endDate
  );
}
