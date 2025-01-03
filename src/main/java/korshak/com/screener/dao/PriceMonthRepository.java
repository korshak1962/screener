package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface PriceMonthRepository extends  JpaRepository<PriceMonth, PriceKey> {
  List<PriceMonth> findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
      String ticker,
      LocalDateTime startDate,
      LocalDateTime endDate
  );
  List<PriceMonth> findByIdTickerOrderByIdDateAsc(String ticker);
}
