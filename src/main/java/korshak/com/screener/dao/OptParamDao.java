package korshak.com.screener.dao;

import java.util.List;
import java.util.Map;

public interface OptParamDao {
  void deleteByTickerAndTimeframe(String ticker, TimeFrame timeframe);

  void saveAll(List<OptParam> optParams);

  void save(OptParam optParam);

  List<OptParam> findByTicker(String ticker);

  Map<String, Double> findValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe);

  Map<String, String> findStringValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe);

  Map<String, Object> findAllValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe);

  // New methods for case_id support
  List<OptParam> findByTickerAndTimeframeAndCaseId(String ticker, TimeFrame timeframe, String caseId);

  Map<String, Double> findValuesByTickerAndTimeframeAndCaseId(String ticker, TimeFrame timeframe, String caseId);

  Map<String, String> findStringValuesByTickerAndTimeframeAndCaseId(String ticker, TimeFrame timeframe, String caseId);

  Map<String, Object> findAllValuesByTickerAndTimeframeAndCaseId(String ticker, TimeFrame timeframe, String caseId);

  Map<String, Double> findValuesByTickerAndStrategy(String ticker, String strategy);

  Map<String, Double> findValuesByTickerAndTimeframeAndStrategyAsMap(String ticker, TimeFrame timeframe,
                                                                     String strategy);

  List<OptParam> findValuesByTickerAndTimeframeAndStrategy(String ticker, TimeFrame timeframe, String strategy);
}