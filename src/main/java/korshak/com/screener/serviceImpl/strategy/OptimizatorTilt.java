package korshak.com.screener.serviceImpl.strategy;

import java.util.HashMap;
import java.util.Map;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.vo.StrategyResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("OptimizatorTilt")
public class OptimizatorTilt extends Optimizator {
  int minLength = 3;
  int maxLength = 12;
  int stepLength = 1;
  double minTiltBuy = 0.0;
  double maxTiltBuy = 0.05;
  double tiltBuyStep = 0.01;
  double minTiltSell = -0.03;
  double maxTiltSell = -0.01;
  double tiltSellStep = 0.01;

  public OptimizatorTilt(@Qualifier("TiltStrategy") TiltStrategy strategy,
                         TradeService tradeService) {
    super(strategy, tradeService);
  }


  @Override
  public Map<String, Double> findOptimumParameters() {
    Map<String, Double> optimumParameters = new HashMap<>();
    double maxPnl = -10E6;
    StrategyResult strategyResultLong;
    StrategyResult strategyResulShort;
    TiltStrategy tiltStrategy = (TiltStrategy) this.strategy;

    for (int tiltLength = minLength; tiltLength < maxLength; tiltLength += stepLength) {
      tiltStrategy.setLength(tiltLength);
      for (double currentTiltBuy = minTiltBuy; currentTiltBuy < maxTiltBuy;
           currentTiltBuy += tiltBuyStep) {
        tiltStrategy.setTiltBuy(currentTiltBuy);
        for (double currentTiltSell = minTiltSell; currentTiltSell < maxTiltSell;
             currentTiltSell += tiltSellStep) {
          tiltStrategy.setTiltSell(currentTiltSell);
          System.out.println("====currentTiltBuy =" + currentTiltBuy);
          System.out.println("====currentTiltSell =" + currentTiltSell);
          strategyResultLong = tradeService.calculateProfitAndDrawdownLong(strategy);
          double longPnL = strategyResultLong.getLongPnL();
          System.out.println("maxPnl= " + maxPnl + " longPnL= " + longPnL + " tiltLength=" +
              tiltLength);

          /*
          strategyResulShort = tradeService.calculateProfitAndDrawdownShort(strategy);
          double shortPnL = strategyResulShort.getShortPnL();
          System.out.println(" shortPnL= " + shortPnL);
           */

          if (maxPnl < longPnL) {
            maxPnl = longPnL;
            optimumParameters.put("TiltBuy", tiltStrategy.getTiltBuy());
            optimumParameters.put("TiltSell", tiltStrategy.getTiltSell());
            optimumParameters.put("Length", (double) tiltStrategy.getLength());
          }
        }
      }
    }
    System.out.println("Optimized maxPnl = " + maxPnl);
    return optimumParameters;
  }
}
