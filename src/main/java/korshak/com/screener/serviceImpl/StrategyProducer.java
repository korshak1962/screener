package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.Map;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.serviceImpl.strategy.BuyAndHoldStrategy;
import korshak.com.screener.serviceImpl.strategy.BuyAndHoldStrategyMinusDownTrend;
import korshak.com.screener.serviceImpl.strategy.BuyCloseHigherPrevClose;
import korshak.com.screener.serviceImpl.strategy.BuyHigherPrevHigh;
import korshak.com.screener.serviceImpl.strategy.DoubleTiltStrategy;
import korshak.com.screener.serviceImpl.strategy.TiltCombinedStrategy;
import korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy;
import korshak.com.screener.serviceImpl.strategy.TiltStrategy;

public class StrategyProducer {
  public StrategyProducer(TimeFrame timeFrame,
                          String ticker,
                          LocalDateTime startDate,
                          LocalDateTime endDate,
                          Map<String, Double> params) {
  }
  public  Strategy getStrategy(String strategyName) {
   return null;
  }
}
