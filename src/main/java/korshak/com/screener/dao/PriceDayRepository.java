package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface PriceDayRepository  extends JpaRepository<PriceDay, PriceKey>  {
  List<PriceDay> findByIdTickerOrderByIdDateAsc(String ticker);
  List<PriceDay> findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
      String ticker,
      LocalDateTime startDate,
      LocalDateTime endDate
  );

}
