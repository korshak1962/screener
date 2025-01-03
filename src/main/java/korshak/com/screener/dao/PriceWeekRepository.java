package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface PriceWeekRepository extends  JpaRepository<PriceWeek, PriceKey> {
  List<PriceWeek> findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
      String ticker,
      LocalDateTime startDate,
      LocalDateTime endDate
  );
  List<PriceWeek> findByIdTickerOrderByIdDateAsc(String ticker);
}
