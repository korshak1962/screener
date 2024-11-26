package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.Map;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.service.TradeService;


public abstract class Optimizator {

  Strategy strategy;
  String ticker;
  TimeFrame timeFrame;
  LocalDateTime startDate;
  LocalDateTime endDate;
  TradeService tradeService;

  public Optimizator(Strategy strategy, TradeService tradeService) {
    this.strategy = strategy;
    this.tradeService = tradeService;
  }

  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.ticker = ticker;
    this.timeFrame = timeFrame;
    this.startDate = startDate;
    this.endDate = endDate;
    strategy.init(ticker,timeFrame,startDate,endDate);
  }

  public abstract Map<String, Double> findOptimumParameters();

}
