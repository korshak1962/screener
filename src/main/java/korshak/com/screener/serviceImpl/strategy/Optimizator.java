package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.Map;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.TradeService;


public abstract class Optimizator {
  public static final String MAX_PNL = "MAX_PNL";
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

  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.ticker = ticker;
    this.timeFrame = timeFrame;
    this.startDate = startDate;
    this.endDate = endDate;
    merger.nameToStrategy.values().forEach(strategy ->
        strategy.init(ticker, timeFrame, startDate, endDate));
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
        parsBest.put("StopLoss",currentPercent);
      }
    }
    return parsBest;
  }

  public abstract Map<String, Double> findOptimumParameters();

}
