package korshak.com.screener.dao;

import java.util.List;
import java.util.Map;

public interface OptParamDao {

  // Save a single OptParam
  OptParam save(OptParam optParam);

  // Save a list of OptParams
  List<OptParam> saveAll(List<OptParam> optParams);

  // Get a specific parameter value
  OptParam get(String ticker, String param, TimeFrame timeframe);

  // Get all parameters for a ticker
  List<OptParam> getAllForTicker(String ticker);

  // Get all parameters for a ticker and timeframe
  List<OptParam> getAllForTickerAndTimeframe(String ticker, TimeFrame timeframe);

  // Convert a map of parameters to OptParam entities and save them
  List<OptParam> saveParamsFromMap(String ticker, TimeFrame timeframe, Map<String, Double> params);

  // Get all parameters for a ticker and timeframe as a map
  Map<String, Double> getParamsAsMap(String ticker, TimeFrame timeframe);

  // Delete all parameters for a ticker
  void deleteForTicker(String ticker);

  // Delete all parameters for a ticker and timeframe
  void deleteForTickerAndTimeframe(String ticker, TimeFrame timeframe);
}