package korshak.com.screener.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OptParamDaoImpl implements OptParamDao {
  private final OptParamRepository optParamRepository;

  @Autowired
  public OptParamDaoImpl(OptParamRepository optParamRepository) {
    this.optParamRepository = optParamRepository;
  }

  @Override
  public void deleteByTickerAndTimeframe(String ticker, TimeFrame timeframe) {
    optParamRepository.deleteById_TickerAndTimeframe(ticker, timeframe);
  }

  @Override
  public void saveAll(List<Param> optParams) {
    optParamRepository.saveAll(optParams);
  }

  @Override
  public void save(Param optParam) {
    optParamRepository.save(optParam);
  }

  @Override
  public List<Param> findByTicker(String ticker) {
    return optParamRepository.findById_Ticker(ticker);
  }

  @Override
  public Map<String, Double> findValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe) {
    List<Param> params = optParamRepository.findById_TickerAndTimeframe(ticker, timeframe);
    return params.stream()
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            Param::getValue
        ));
  }

  @Override
  public Map<String, String> findStringValuesByTickerAndTimeframe(String ticker,
                                                                  TimeFrame timeframe) {
    List<Param> params = optParamRepository.findById_TickerAndTimeframe(ticker, timeframe);
    return params.stream()
        .filter(param -> param.getValueString() != null)
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            Param::getValueString
        ));
  }

  @Override
  public Map<String, Object> findAllValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe) {
    List<Param> params = optParamRepository.findById_TickerAndTimeframe(ticker, timeframe);
    Map<String, Object> result = new HashMap<>();

    for (Param param : params) {
      String paramName = param.getId().getParam();
      result.put(paramName, param.getValue());
      result.put(paramName, param.getValueString());
    }

    return result;
  }

  @Override
  public Map<String, Double> findValuesByTickerAndStrategy(String ticker, String strategy) {
    List<Param> params = optParamRepository.findById_TickerAndId_Strategy(ticker, strategy);
    return params.stream()
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            Param::getValue
        ));
  }

  @Override
  public Map<String, Double> findValuesByTickerAndTimeframeAndStrategyAsMap(String ticker,
                                                                            TimeFrame timeframe,
                                                                            String strategy) {
    List<Param> params =
        findValuesByTickerAndTimeframeAndStrategy(ticker, timeframe, strategy);
    return params.stream()
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            Param::getValue
        ));
  }

  public List<Param> findValuesByTickerAndTimeframeAndStrategy(String ticker,
                                                               TimeFrame timeframe,
                                                               String strategy) {
    List<Param> params = optParamRepository.findById_TickerAndTimeframeAndId_Strategy(ticker,
        timeframe, strategy);
    return params;
  }

  // Methods for case_id
  @Override
  public List<Param> findByTickerAndTimeframeAndCaseId(String ticker, TimeFrame timeframe,
                                                       String caseId) {
    return optParamRepository.findById_TickerAndTimeframeAndId_CaseId(ticker, timeframe, caseId);
  }

  @Override
  public List<Param> findByCaseId(String caseId) {
    return optParamRepository.findById_CaseId(caseId);
  }

  @Override
  public List<Param> findByTickerAndCaseId(String ticker, String caseId) {
    return optParamRepository.findById_TickerAndId_CaseId(ticker, caseId);
  }

  @Override
  public Map<String, Double> findValuesByTickerAndTimeframeAndCaseId(String ticker,
                                                                     TimeFrame timeframe,
                                                                     String caseId) {
    List<Param> params =
        optParamRepository.findById_TickerAndTimeframeAndId_CaseId(ticker, timeframe, caseId);
    return params.stream()
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            Param::getValue
        ));
  }

  @Override
  public Map<String, String> findStringValuesByTickerAndTimeframeAndCaseId(String ticker,
                                                                           TimeFrame timeframe,
                                                                           String caseId) {
    List<Param> params =
        optParamRepository.findById_TickerAndTimeframeAndId_CaseId(ticker, timeframe, caseId);
    return params.stream()
        .filter(param -> param.getValueString() != null)
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            Param::getValueString
        ));
  }

  @Override
  public Map<String, Object> findAllValuesByTickerAndTimeframeAndCaseId(String ticker,
                                                                        TimeFrame timeframe,
                                                                        String caseId) {
    List<Param> params =
        optParamRepository.findById_TickerAndTimeframeAndId_CaseId(ticker, timeframe, caseId);
    Map<String, Object> result = new HashMap<>();
    for (Param param : params) {
      String paramName = param.getId().getParam();
      result.put(paramName, param.getValue());
      result.put(paramName, param.getValueString());
    }
    return result;
  }

  // New methods for strategyClass
  @Override
  public List<Param> findByStrategyClass(String strategyClass) {
    return optParamRepository.findByStrategyClass(strategyClass);
  }

  @Override
  public List<Param> findByTickerAndStrategyClass(String ticker, String strategyClass) {
    return optParamRepository.findById_TickerAndStrategyClass(ticker, strategyClass);
  }

  @Override
  public List<Param> findByTickerAndTimeframeAndStrategyClass(String ticker, TimeFrame timeframe,
                                                              String strategyClass) {
    return optParamRepository.findById_TickerAndTimeframeAndStrategyClass(ticker, timeframe,
        strategyClass);
  }

  @Override
  public Map<String, Double> findValuesByTickerAndTimeframeAndStrategyClass(String ticker,
                                                                            TimeFrame timeframe,
                                                                            String strategyClass) {
    List<Param> params =
        optParamRepository.findById_TickerAndTimeframeAndStrategyClass(ticker, timeframe,
            strategyClass);
    return params.stream()
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            Param::getValue
        ));
  }

  @Override
  public Map<String, String> findStringValuesByTickerAndTimeframeAndStrategyClass(String ticker,
                                                                                  TimeFrame timeframe,
                                                                                  String strategyClass) {
    List<Param> params =
        optParamRepository.findById_TickerAndTimeframeAndStrategyClass(ticker, timeframe,
            strategyClass);
    return params.stream()
        .filter(param -> param.getValueString() != null)
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            Param::getValueString
        ));
  }

  @Override
  public Map<String, Object> findAllValuesByTickerAndTimeframeAndStrategyClass(String ticker,
                                                                               TimeFrame timeframe,
                                                                               String strategyClass) {
    List<Param> params =
        optParamRepository.findById_TickerAndTimeframeAndStrategyClass(ticker, timeframe,
            strategyClass);
    Map<String, Object> result = new HashMap<>();

    for (Param param : params) {
      String paramName = param.getId().getParam();
      result.put(paramName, param.getValue());
      result.put(paramName, param.getValueString());
    }

    return result;
  }

  @Override
  public Map<String, List<Param>> findByTickerAndCaseIdGroupedByStrategy(String ticker,
                                                                         String caseId) {
    // Get all parameters for the ticker, timeframe, and tag
    List<Param> allParams = findByTickerAndCaseId(ticker, caseId);
    // Group parameters by strategy
    Map<String, List<Param>> strategyToParams = new HashMap<>();
    for (Param param : allParams) {
      String strategy = param.getId().getStrategy();
      // Initialize the list if this is the first parameter for this strategy
      if (!strategyToParams.containsKey(strategy)) {
        strategyToParams.put(strategy, new ArrayList<>());
      }
      // Add the parameter to the list for this strategy
      strategyToParams.get(strategy).add(param);
    }
    return strategyToParams;
  }
}