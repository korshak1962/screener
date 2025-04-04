package korshak.com.screener.serviceImpl.strategy;

import static korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy.LENGTH;
import static korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy.TILT_BUY;
import static korshak.com.screener.serviceImpl.strategy.TiltFromBaseStrategy.TILT_SELL;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import korshak.com.screener.dao.OptParam;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.utils.Utils;
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
  @Autowired
  @Qualifier("TiltFromBaseStrategy")
  TiltFromBaseStrategy tiltFromBaseStrategy;
  Map<String, Double> optimumParameters;

  public OptimizatorTilt(@Qualifier("StrategyMerger") StrategyMerger strategyMerger,
                         TradeService tradeService) {
    super(strategyMerger, tradeService);
  }

  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.merger.addStrategy(tiltFromBaseStrategy,timeFrame);
    super.init(ticker, timeFrame, startDate, endDate);
  }

  @Override
  public Map<String, Double> findOptimumParameters() {
    optimazeTiltStrategy();
    return optimumParameters;
  }

  @Override
  public Map<Strategy, Map<String, OptParam>> findOptimalParametersForAllStrategies() {
    throw new RuntimeException("Not implemented");
  }

  public void configureTiltFromBaseStrategy(List<OptParam> optParamList ) {
    Map<String, OptParam> optParamsMap=Utils.getOptParamsAsMap(optParamList);
    OptParam length=optParamsMap.get(LENGTH);
    this.minLength = (int)length.getMin();
    this.maxLength = (int)length.getMax();
    this.stepLength = (int)length.getStep();
    OptParam tiltBuy=optParamsMap.get(TILT_BUY);
    this.minTiltBuy = tiltBuy.getMin();
    this.maxTiltBuy = tiltBuy.getMax();
    this.tiltBuyStep = tiltBuy.getStep();
    OptParam tiltSell=optParamsMap.get(TILT_SELL);
    this.minTiltSell = tiltSell.getMin();
    this.maxTiltSell = tiltSell.getMax();
    this.tiltSellStep = tiltSell.getStep();
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
          //System.out.println("====tiltLength =" + tiltLength);
          //System.out.println("====currentTiltBuy =" + currentTiltBuy);
          //System.out.println("====currentTiltSell =" + currentTiltSell);
          merger.mergeSignals();
          strategyResultLong = tradeService.calculateProfitAndDrawdownLong(merger);
          if (strategyResultLong == null) {
            return;
          }
          double longPnL = strategyResultLong.getLongPnL();
          //System.out.println("maxPnl= " + maxPnl + " longPnL= " + longPnL + " tiltLength=" +
          //    tiltLength);

          /*
          strategyResulShort = tradeService.calculateProfitAndDrawdownShort(strategy);
          double shortPnL = strategyResulShort.getShortPnL();
          System.out.println(" shortPnL= " + shortPnL);
           */

          if (maxPnl < longPnL) {
            strategyResultLong.getTradesLong().forEach(trade -> {
              System.out.println(trade);
            });
            maxPnl = longPnL;
            optimumParameters.put(MAX_PNL, maxPnl);
            optimumParameters.put(TILT_BUY, tiltFromBaseStrategy.getTiltBuy());
            System.out.println("tiltBuy = "+tiltFromBaseStrategy.getTiltBuy());
            optimumParameters.put(TILT_SELL, tiltFromBaseStrategy.getTiltSell());
            System.out.println("tiltSell = "+tiltFromBaseStrategy.getTiltSell());
            optimumParameters.put(LENGTH, (double) tiltFromBaseStrategy.getLength());
            System.out.println("LENGTH = "+tiltFromBaseStrategy.getLength());
          }
        }
      }
    }
    //   System.out.println("Optimized maxPnl = " + maxPnl);
  }
}
