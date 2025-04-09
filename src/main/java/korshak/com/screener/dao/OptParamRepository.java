package korshak.com.screener.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OptParamRepository extends JpaRepository<Param, Param.OptParamKey> {
  // Existing methods
  void deleteById_TickerAndTimeframe(String ticker, TimeFrame timeframe);

  List<Param> findById_Ticker(String ticker);

  List<Param> findById_TickerAndTimeframe(String ticker, TimeFrame timeframe);

  List<Param> findById_TickerAndId_Strategy(String ticker, String strategy);

  List<Param> findById_TickerAndTimeframeAndId_Strategy(String ticker, TimeFrame timeframe, String strategy);

  // Methods for case_id
  List<Param> findById_TickerAndTimeframeAndId_CaseId(String ticker, TimeFrame timeframe, String caseId);

  List<Param> findById_TickerAndTimeframeAndId_StrategyAndId_CaseId(String ticker, TimeFrame timeframe, String strategy, String caseId);

  // New methods for finding by caseId
  List<Param> findById_CaseId(String caseId);

  List<Param> findById_TickerAndId_CaseId(String ticker, String caseId);

  // New methods for strategyClass
  List<Param> findByStrategyClass(String strategyClass);

  List<Param> findById_TickerAndStrategyClass(String ticker, String strategyClass);

  List<Param> findById_TickerAndTimeframeAndStrategyClass(String ticker, TimeFrame timeframe, String strategyClass);

  List<Param> findById_TickerAndTimeframeAndId_StrategyAndStrategyClass(String ticker, TimeFrame timeframe, String strategy, String strategyClass);

  List<Param> findById_TickerAndTimeframeAndId_CaseIdAndStrategyClass(String ticker, TimeFrame timeframe, String caseId, String strategyClass);
}