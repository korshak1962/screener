package korshak.com.screener.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OptParamRepository extends JpaRepository<OptParam, OptParam.OptParamKey> {
  // Update query methods to use the embedded key
  void deleteById_TickerAndTimeframe(String ticker, TimeFrame timeframe);

  List<OptParam> findById_Ticker(String ticker);

  List<OptParam> findById_TickerAndTimeframe(String ticker, TimeFrame timeframe);

  List<OptParam> findById_TickerAndId_Strategy(String ticker, String strategy);

  List<OptParam> findById_TickerAndTimeframeAndId_Strategy(String ticker, TimeFrame timeframe, String strategy);

// ==== OptParamRepository.java changes ====

  // Add new methods to query including case_id
  List<OptParam> findById_TickerAndTimeframeAndId_CaseId(String ticker, TimeFrame timeframe, String caseId);

  List<OptParam> findById_TickerAndTimeframeAndId_StrategyAndId_CaseId(String ticker, TimeFrame timeframe, String strategy, String caseId);

}