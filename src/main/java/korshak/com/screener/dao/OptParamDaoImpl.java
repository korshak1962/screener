package korshak.com.screener.dao;

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
  public void saveAll(List<OptParam> optParams) {
    optParamRepository.saveAll(optParams);
  }

  @Override
  public void save(OptParam optParam) {
    optParamRepository.save(optParam);
  }

  @Override
  public List<OptParam> findByTicker(String ticker) {
    return optParamRepository.findById_Ticker(ticker);
  }

  @Override
  public Map<String, Double> findValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe) {
    List<OptParam> params = optParamRepository.findById_TickerAndTimeframe(ticker, timeframe);
    return params.stream()
        .filter(param -> param.getValue() != null)
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            OptParam::getValue
        ));
  }

  @Override
  public Map<String, String> findStringValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe) {
    List<OptParam> params = optParamRepository.findById_TickerAndTimeframe(ticker, timeframe);
    return params.stream()
        .filter(param -> param.getValueString() != null)
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            OptParam::getValueString
        ));
  }

  @Override
  public Map<String, Object> findAllValuesByTickerAndTimeframe(String ticker, TimeFrame timeframe) {
    List<OptParam> params = optParamRepository.findById_TickerAndTimeframe(ticker, timeframe);
    Map<String, Object> result = new HashMap<>();

    for (OptParam param : params) {
      String paramName = param.getId().getParam();
      if (param.getValue() != null) {
        result.put(paramName, param.getValue());
      } else if (param.getValueString() != null) {
        result.put(paramName, param.getValueString());
      }
    }

    return result;
  }

  @Override
  public Map<String, Double> findValuesByTickerAndStrategy(String ticker, String strategy) {
    List<OptParam> params = optParamRepository.findById_TickerAndId_Strategy(ticker, strategy);
    return params.stream()
        .filter(param -> param.getValue() != null)
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            OptParam::getValue
        ));
  }

  @Override
  public Map<String, Double> findValuesByTickerAndTimeframeAndStrategy(String ticker, TimeFrame timeframe, String strategy) {
    List<OptParam> params = optParamRepository.findById_TickerAndTimeframeAndId_Strategy(ticker, timeframe, strategy);
    return params.stream()
        .filter(param -> param.getValue() != null)
        .collect(Collectors.toMap(
            param -> param.getId().getParam(),
            OptParam::getValue
        ));
  }
}