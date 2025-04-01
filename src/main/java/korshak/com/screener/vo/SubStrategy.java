package korshak.com.screener.vo;

import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;

public class SubStrategy {

  Strategy strategy;
  TimeFrame timeFrame;

  public SubStrategy(Strategy strategy, TimeFrame timeFrame) {
    this.strategy = strategy;
    this.timeFrame = timeFrame;
  }

  public Strategy getStrategy() {
    return strategy;
  }

  public TimeFrame getTimeFrame() {
    return timeFrame;
  }
}
