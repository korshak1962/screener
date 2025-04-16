package korshak.com.screener.serviceImpl.strategy;

import java.util.HashMap;
import java.util.Map;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.Param;
import korshak.com.screener.service.strategy.Configurable;
import korshak.com.screener.service.strategy.PostTradeStrategy;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;

public class SimpleStoplossStrategy implements PostTradeStrategy, Configurable {
  Map<String, Param> paramsMap = new HashMap<>();
  public static final String STOP_LOSS_PERCENT = "StopLoss";
  private float stopLossPercent = 1;

  @Override
  public Signal getPostTradeSignal(BasePrice price, Signal lastSignal) {
    // stopLoss
    // System.out.println("stopLossMaxPercent = " + stopLossMaxPercent);
    Signal signalStopLoss = null;
    if (lastSignal != null && lastSignal.getSignalType() == SignalType.LongOpen &&
        price.getLow() < stopLossPercent * lastSignal.getPrice()) {
      signalStopLoss = Utils.createSignal(price, SignalType.LongClose,
          stopLossPercent * lastSignal.getPrice(), "stop loss");
    }
    return signalStopLoss;
  }

  public void setStopLossPercent(float stopLossPercent) {
    this.stopLossPercent = stopLossPercent;
  }

  public float getStopLossPercent() {
    return stopLossPercent;
  }

  @Override
  public Map<String, Param> getParams() {
    return paramsMap;
  }

  @Override
  public void configure(Map<String, Param> nameToParam) {
    if (nameToParam != null && nameToParam.get(STOP_LOSS_PERCENT) != null) {
      this.setStopLossPercent((float)nameToParam.get(STOP_LOSS_PERCENT).getValue());
    } else {
      throw new RuntimeException(
          "No opt params for strategy = " + this.getClass().getSimpleName());
    }
    this.paramsMap = nameToParam;
  }
}
