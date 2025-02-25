package korshak.com.screener.dao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptParamRepository extends JpaRepository<OptParam, OptParamKey> {

  // Find by ticker, param, and timeframe
  OptParam findByTickerAndParamAndTimeframe(String ticker, String param, TimeFrame timeframe);

  // Find all by ticker
  List<OptParam> findByTicker(String ticker);

  // Find all by ticker and timeframe
  List<OptParam> findByTickerAndTimeframe(String ticker, TimeFrame timeframe);

  // Delete by ticker
  void deleteByTicker(String ticker);

  // Delete by ticker and timeframe
  void deleteByTickerAndTimeframe(String ticker, TimeFrame timeframe);
}