package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.Map;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.service.TradeService;


public abstract class Optimizator {

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
    merger.init(ticker,timeFrame,startDate,endDate);
    merger.nameToStrategy.values().forEach(strategy -> strategy.init(ticker,timeFrame,startDate,endDate));
  }

  public abstract Map<String, Double> findOptimumParameters();

}
