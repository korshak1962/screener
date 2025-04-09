package korshak.com.screener.dao;

import java.util.List;
import java.util.Map;

public interface OptParamDao {
  void deleteByTickerAndTimeframe(String ticker, TimeFrame timeframe);

  void saveAll(List<Param> optParams);

  void save(Param optParam);

  List<Param> findByTicker(String ticker);

  Map<String, Double> findValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe);

  Map<String, String> findStringValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe);

  Map<String, Object> findAllValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe);

  // Methods for case_id support
  List<Param> findByTickerAndTimeframeAndCaseId(String ticker, TimeFrame timeframe, String caseId);

  Map<String, Double> findValuesByTickerAndTimeframeAndCaseId(String ticker, TimeFrame timeframe, String caseId);

  Map<String, String> findStringValuesByTickerAndTimeframeAndCaseId(String ticker, TimeFrame timeframe, String caseId);

  Map<String, Object> findAllValuesByTickerAndTimeframeAndCaseId(String ticker, TimeFrame timeframe, String caseId);

  // New methods for finding by caseId
  List<Param> findByCaseId(String caseId);

  List<Param> findByTickerAndCaseId(String ticker, String caseId);

  Map<String, Double> findValuesByTickerAndStrategy(String ticker, String strategy);

  Map<String, Double> findValuesByTickerAndTimeframeAndStrategyAsMap(String ticker, TimeFrame timeframe,
                                                                     String strategy);

  List<Param> findValuesByTickerAndTimeframeAndStrategy(String ticker, TimeFrame timeframe, String strategy);

  // New methods for strategyClass
  List<Param> findByStrategyClass(String strategyClass);

  List<Param> findByTickerAndStrategyClass(String ticker, String strategyClass);

  List<Param> findByTickerAndTimeframeAndStrategyClass(String ticker, TimeFrame timeframe, String strategyClass);

  Map<String, Double> findValuesByTickerAndTimeframeAndStrategyClass(String ticker, TimeFrame timeframe, String strategyClass);

  Map<String, String> findStringValuesByTickerAndTimeframeAndStrategyClass(String ticker, TimeFrame timeframe, String strategyClass);

  Map<String, Object> findAllValuesByTickerAndTimeframeAndStrategyClass(String ticker, TimeFrame timeframe, String strategyClass);

  /**
   * Find optimization parameters grouped by strategy for a specific ticker and caseId
   *
   * @param ticker The stock ticker
   * @param caseId The caseId value
   * @return Map of strategy names to their corresponding parameter lists
   */
  Map<String, List<Param>> findByTickerAndCaseIdGroupedByStrategy(String ticker, String caseId);
}