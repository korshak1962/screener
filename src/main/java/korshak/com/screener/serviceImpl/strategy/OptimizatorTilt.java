package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.vo.StrategyResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("OptimizatorTilt")
public class OptimizatorTilt extends Optimizator {
  int minLength = 30;
  int maxLength = 40;
  int stepLength = 1;
  double minTiltBuy = 0.0;
  double maxTiltBuy = 0.05;
  double tiltBuyStep = 0.01;
  double minTiltSell = -0.03;
  double maxTiltSell = -0.01;
  double tiltSellStep = 0.01;

  public OptimizatorTilt(@Qualifier("StrategyMerger") StrategyMerger strategyMerger,
                         TradeService tradeService) {
    super(strategyMerger, tradeService);
  }

  @Autowired
  @Qualifier("TiltFromBaseStrategy")
  TiltFromBaseStrategy tiltFromBaseStrategy;
  Map<String, Double> optimumParameters;

  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.merger.addStrategy(tiltFromBaseStrategy);
    super.init(ticker, timeFrame, startDate, endDate);
  }

  @Override
  public Map<String, Double> findOptimumParameters() {
    optimazeTiltStrategy();
    return optimumParameters;
  }

  public void configure(int minLength, int maxLength, int stepLength,
                        double minTiltBuy, double maxTiltBuy, double tiltBuyStep,
                        double minTiltSell, double maxTiltSell, double tiltSellStep) {
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.stepLength = stepLength;
    this.minTiltBuy = minTiltBuy;
    this.maxTiltBuy = maxTiltBuy;
    this.tiltBuyStep = tiltBuyStep;
    this.minTiltSell = minTiltSell;
    this.maxTiltSell = maxTiltSell;
    this.tiltSellStep = tiltSellStep;
  }

  private void optimazeTiltStrategy() {
    optimumParameters = new HashMap<>();
    double maxPnl = -10E6;
    StrategyResult strategyResultLong;
    StrategyResult strategyResulShort;
    for (int tiltLength = minLength; tiltLength <= maxLength; tiltLength += stepLength) {
      tiltFromBaseStrategy.setLength(tiltLength);
      for (double currentTiltBuy = minTiltBuy; currentTiltBuy <= maxTiltBuy;
           currentTiltBuy += tiltBuyStep) {
        tiltFromBaseStrategy.setTiltBuy(currentTiltBuy);
        for (double currentTiltSell = minTiltSell; currentTiltSell <= maxTiltSell;
             currentTiltSell += tiltSellStep) {
          tiltFromBaseStrategy.setTiltSell(currentTiltSell);
          System.out.println("====tiltLength =" + tiltLength);
          System.out.println("====currentTiltBuy =" + currentTiltBuy);
          System.out.println("====currentTiltSell =" + currentTiltSell);
          merger.mergeSignals();
          strategyResultLong = tradeService.calculateProfitAndDrawdownLong(merger);
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
            optimumParameters.put("TiltBuy", tiltFromBaseStrategy.getTiltBuy());
            optimumParameters.put("TiltSell", tiltFromBaseStrategy.getTiltSell());
            optimumParameters.put("Length", (double) tiltFromBaseStrategy.getLength());
          }
        }
      }
    }
    System.out.println("Optimized maxPnl = " + maxPnl);
  }
}
