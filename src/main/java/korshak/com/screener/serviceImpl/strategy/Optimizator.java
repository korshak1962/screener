package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.Map;
import korshak.com.screener.dao.OptParam;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.service.strategy.Strategy;


public abstract class Optimizator {
  public static final String MAX_PNL = "MAX_PNL";
  public static final String STOP_LOSS = "StopLoss";
  StrategyMerger merger;
  String ticker;
  TimeFrame timeFrame;
  LocalDateTime startDate;
  LocalDateTime endDate;
  TradeService tradeService;

  public Optimizator(StrategyMerger merger, TradeService tradeService) {
    this.merger = merger;
    this.tradeService = tradeService;
  }

  public void init(StrategyMerger merger) {
    this.ticker = merger.getTicker();
    this.timeFrame = merger.timeFrame;
    this.startDate = merger.startDate;
    this.endDate = merger.endDate;
    merger.getSubStrategies().forEach(subStrategy ->
        subStrategy.init(ticker, timeFrame, startDate, endDate));
  }

  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.ticker = ticker;
    this.timeFrame = timeFrame;
    this.startDate = startDate;
    this.endDate = endDate;
    merger.getSubStrategies().forEach(subStrategy ->
        subStrategy.init(ticker, timeFrame, startDate, endDate));
    // Then initialize merger
    merger.init(ticker, timeFrame, startDate, endDate);
  }

  public Map<String, Double> findOptimumParametersWithStopLoss(double minPercent, double maxPercent,
                                                               double step) {
    Map<String, Double> parsBest = null;
    for (double currentPercent = minPercent; currentPercent <= maxPercent;
         currentPercent += step) {
      merger.setStopLossPercent(currentPercent);
      Map<String, Double> pars = findOptimumParameters();
      if (parsBest == null || parsBest.get(MAX_PNL) < pars.get(MAX_PNL)) {
        parsBest = pars;
        parsBest.put(STOP_LOSS, currentPercent);
      }
    }
    return parsBest;
  }

  public abstract Map<String, Double> findOptimumParameters();

  public abstract Map<Strategy, Map<String, OptParam>> findOptimalParametersForAllStrategies();
}
