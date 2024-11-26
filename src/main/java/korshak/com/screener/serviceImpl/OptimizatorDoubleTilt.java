package korshak.com.screener.serviceImpl;

import java.util.HashMap;
import java.util.Map;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.vo.StrategyResult;
import org.springframework.stereotype.Service;

@Service
public class OptimizatorDoubleTilt extends Optimizator {

  public OptimizatorDoubleTilt(DoubleTiltStrategy strategy, TradeService tradeService) {
    super(strategy, tradeService);
  }

  int minShortLength = 1;
  int maxShortLength = 15;
  int numStepsShortStep = 2;
  int minTiltShortBuy = 0;
  int maxTiltShortBuy = 1;
  int numStepsTiltShortBuy = 10;
  int minTiltShortSell = -1;
  int maxTiltShortSell = 0;
  int numStepsTiltShortSell = 10;

  @Override
  public Map<String, Double> findOptimumParameters() {
    Map<String, Double> optimumParameters = new HashMap<>();
    Double maxPnl = Double.MIN_VALUE;
    StrategyResult strategyResultDoubleTilt;
    DoubleTiltStrategy fullDoubleTiltStrategy = (DoubleTiltStrategy) this.strategy;
    fullDoubleTiltStrategy.setTiltPeriod(5);
    fullDoubleTiltStrategy.setLongLength(45);

    fullDoubleTiltStrategy.setTiltShortBuy(.02);
    fullDoubleTiltStrategy.setTiltShortSell(-.02);
    fullDoubleTiltStrategy.setTiltLongBuy(-100);
    fullDoubleTiltStrategy.setTiltLongSell(-200);
    for (int currentShortLength = minShortLength; currentShortLength < maxShortLength;
         currentShortLength += numStepsShortStep) {
      fullDoubleTiltStrategy.setShortLength(currentShortLength);
      strategyResultDoubleTilt =
          tradeService.calculateProfitAndDrawdown(strategy,
              ticker,
              startDate,
              endDate,
              timeFrame);
      System.out.println();
      double longPnL = strategyResultDoubleTilt.getLongPnL();
      System.out.println("maxPnl= " + maxPnl + " longPnL= " + longPnL + " currentShortLength=" +
          currentShortLength);
      if (maxPnl < longPnL) {
        maxPnl = longPnL;
        optimumParameters.put("ShortLength", (double) currentShortLength);
      }
    }
    return optimumParameters;
  }


}
