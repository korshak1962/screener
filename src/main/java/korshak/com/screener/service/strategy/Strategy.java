package korshak.com.screener.service.strategy;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.OptParam;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.vo.Signal;

public interface Strategy {
  List<? extends Signal> getSignalsLong();

  List<? extends Signal> getSignalsShort();

  Strategy init(String ticker, TimeFrame timeFrame, LocalDateTime startDate, LocalDateTime endDate);

  String getStrategyName();

  List<? extends BasePrice> getPrices();

  Map<String, NavigableMap<LocalDateTime, Double>> getIndicators();

  Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators();

  TimeFrame getTimeFrame();

  String getTicker();

  LocalDateTime getStartDate();

  LocalDateTime getEndDate();

  List<Signal> getAllSignals();

  List<Signal> getAllSignals(TimeFrame timeFrame);

  void calcSignals();

  /**
   * Returns a map of parameter names to OptParam objects that can be optimized.
   * Default implementation returns an empty map.
   *
   * @return Map of parameter names to OptParam objects
   */
  default Map<String, OptParam> getOptParams() {
    return new HashMap<>();
  }

  /**
   * Sets optimization parameters for the strategy.
   * Default implementation does nothing.
   * Implementing classes should apply these parameters to their internal state.
   *
   * @param optParamsMap Map of parameter names to OptParam objects
   */
  default void setOptParams(Map<String, OptParam> optParamsMap) {
    // Default implementation does nothing
  }
}
