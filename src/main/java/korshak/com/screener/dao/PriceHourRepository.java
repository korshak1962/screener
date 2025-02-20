package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface PriceHourRepository extends JpaRepository<PriceHour, PriceKey> {
  List<PriceHour> findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
      String ticker,
      LocalDateTime startDate,
      LocalDateTime endDate
  );

  List<PriceHour> findByIdTickerOrderByIdDateAsc(String ticker);
}
