package korshak.com.screener.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmaWeekRepository extends JpaRepository<SmaWeek, SmaKey> {
  void deleteByIdTickerAndIdLength(String ticker, int length);
}
