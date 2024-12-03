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
  int maxShortLength = 16;
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
    double maxPnl = -10E6;
    StrategyResult strategyResultDoubleTilt;
    DoubleTiltStrategy fullDoubleTiltStrategy = (DoubleTiltStrategy) this.strategy;
    fullDoubleTiltStrategy.setTiltPeriod(5);
    fullDoubleTiltStrategy.setTrendLengthSma(15);

    fullDoubleTiltStrategy.setTiltLongOpen(.02);
    fullDoubleTiltStrategy.setTiltLongClose(-.02);
    fullDoubleTiltStrategy.setTiltHigherTrendLong(-100);
    fullDoubleTiltStrategy.setTiltHigherTrendShort(-200);
    for (int currentShortLength = minShortLength; currentShortLength < maxShortLength;
         currentShortLength += numStepsShortStep) {
      fullDoubleTiltStrategy.setSmaLength(currentShortLength);
      strategyResultDoubleTilt =
          tradeService.calculateProfitAndDrawdownLong(strategy,
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
