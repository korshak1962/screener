package korshak.com.screener.serviceImpl.strategy;

import java.util.HashMap;
import java.util.Map;
import korshak.com.screener.dao.OptParam;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.service.strategy.Strategy;
import org.springframework.stereotype.Service;

@Service("OptimizatorDoubleTilt")
public class OptimizatorDoubleTilt extends Optimizator {

  int minShortLength = 1;
  int maxShortLength = 16;
  int numStepsShortStep = 1;
  int minTiltShortBuy = 0;
  int maxTiltShortBuy = 1;
  int numStepsTiltShortBuy = 10;
  int minTiltShortSell = -1;
  int maxTiltShortSell = 0;
  int numStepsTiltShortSell = 10;

  public OptimizatorDoubleTilt(StrategyMerger strategy, TradeService tradeService) {
    super(strategy, tradeService);
  }

  @Override
  public Map<String, Double> findOptimumParameters() {

    Map<String, Double> optimumParameters = new HashMap<>();
    /*
    double maxPnl = -10E6;
    StrategyResult strategyResultDoubleTiltLong;
    StrategyResult strategyResultDoubleTiltShort;
    DoubleTiltStrategy fullDoubleTiltStrategy = (DoubleTiltStrategy) this.merger;
    //fullDoubleTiltStrategy.setTiltPeriod(5);
    fullDoubleTiltStrategy.setTrendLengthSma(15);

    fullDoubleTiltStrategy.setTiltLongOpen(.02);
    double minTiltLongOpen = -0.2;
    double maxTiltLongOpen = 0.0;
    double tiltLongOpenStep = 0.1;
    fullDoubleTiltStrategy.setTiltLongClose(-.02);
    fullDoubleTiltStrategy.setTiltShortOpen(-.01);
    fullDoubleTiltStrategy.setTiltShortClose(-.0);


    fullDoubleTiltStrategy.setTiltHigherTrendLong(-1);
    fullDoubleTiltStrategy.setTiltHigherTrendShort(-1);

    for (double currentTiltLongOpen = minTiltLongOpen; currentTiltLongOpen < maxTiltLongOpen;
         currentTiltLongOpen += tiltLongOpenStep) {
      System.out.println("====currentTiltLongOpen =" + currentTiltLongOpen);
      for (int currentShortLength = minShortLength; currentShortLength < maxShortLength;
           currentShortLength += numStepsShortStep) {
        fullDoubleTiltStrategy.setSmaLength(currentShortLength);
        strategyResultDoubleTiltLong =
            tradeService.calculateProfitAndDrawdownLong(merger);

        double longPnL = strategyResultDoubleTiltLong.getLongPnL();
        System.out.println("maxPnl= " + maxPnl + " longPnL= " + longPnL + " currentShortLength=" +
            currentShortLength);

        strategyResultDoubleTiltShort = tradeService.calculateProfitAndDrawdownShort(merger);
        double shortPnL = strategyResultDoubleTiltShort.getShortPnL();
        System.out.println(" shortPnL= " + shortPnL);

        if (maxPnl < longPnL) {
          maxPnl = longPnL;
          optimumParameters.put("ShortLength", (double) currentShortLength);
          optimumParameters.put("optimumTiltLongOpen", currentTiltLongOpen);
        }
      }
    }

     */
    return optimumParameters;
  }

  @Override
  public Map<Strategy, Map<String, OptParam>> findOptimalParametersForAllStrategies() {
    throw new UnsupportedOperationException("Unimplemented method 'findOptimalParametersForAllStrategies'");
  }


}
