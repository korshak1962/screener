package korshak.com.screener.serviceImpl.strategy;

import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.OptParamDao;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("StopLossPercentStrategy")
public class StopLossPercentStrategy extends BaseStrategy {
  BasePrice pricePrev;
  double stopLossPercent = .03;

  public StopLossPercentStrategy(PriceDao priceDao, OptParamDao optParamDao) {
    super(priceDao, optParamDao);
  }

  @Override
  public Signal getSignal(BasePrice price) {
    if (pricePrev == null) {
      pricePrev = price;
      return null;
    }
    if ((price.getLow() - pricePrev.getLow()) < -stopLossPercent * pricePrev.getLow()) {
      pricePrev = price;
      return Utils.createSignal(price, SignalType.LongClose, "stop loss");
    }
    return null;
  }

  @Override
  public Signal getSignal(BasePrice priceOfBackupTimeframe, BasePrice price) {
    pricePrev = priceOfBackupTimeframe;
    return getSignal(price);
  }

  public double getStopLossPercent() {
    return stopLossPercent;
  }

  public void setStopLossPercent(double stopLossPercent) {
    this.stopLossPercent = stopLossPercent;
  }
}
