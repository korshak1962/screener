package korshak.com.screener.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OptParamDaoImpl implements OptParamDao {

  private final OptParamRepository optParamRepository;

  @Autowired
  public OptParamDaoImpl(OptParamRepository optParamRepository) {
    this.optParamRepository = optParamRepository;
  }

  @Override
  public OptParam save(OptParam optParam) {
    return optParamRepository.save(optParam);
  }

  @Override
  public List<OptParam> saveAll(List<OptParam> optParams) {
    return optParamRepository.saveAll(optParams);
  }

  @Override
  public OptParam get(String ticker, String param, TimeFrame timeframe) {
    return optParamRepository.findByTickerAndParamAndTimeframe(ticker, param, timeframe);
  }

  @Override
  public List<OptParam> getAllForTicker(String ticker) {
    return optParamRepository.findByTicker(ticker);
  }

  @Override
  public List<OptParam> getAllForTickerAndTimeframe(String ticker, TimeFrame timeframe) {
    return optParamRepository.findByTickerAndTimeframe(ticker, timeframe);
  }

  @Override
  @Transactional
  public List<OptParam> saveParamsFromMap(String ticker, TimeFrame timeframe,
                                          Map<String, Double> params) {
    List<OptParam> optParams = new ArrayList<>();

    for (Map.Entry<String, Double> entry : params.entrySet()) {
      OptParam optParam = new OptParam(ticker, entry.getKey(), timeframe, entry.getValue());
      optParams.add(optParam);
    }

    return optParamRepository.saveAll(optParams);
  }

  @Override
  public Map<String, Double> getParamsAsMap(String ticker, TimeFrame timeframe) {
    List<OptParam> params = optParamRepository.findByTickerAndTimeframe(ticker, timeframe);
    Map<String, Double> result = new HashMap<>();

    for (OptParam param : params) {
      result.put(param.getParam(), param.getValue());
    }

    return result;
  }

  @Override
  @Transactional
  public void deleteForTicker(String ticker) {
    optParamRepository.deleteByTicker(ticker);
  }

  @Override
  @Transactional
  public void deleteForTickerAndTimeframe(String ticker, TimeFrame timeframe) {
    optParamRepository.deleteByTickerAndTimeframe(ticker, timeframe);
  }
}