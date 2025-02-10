package korshak.com.screener.serviceImpl.strategy;

import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("StopLossPercentStrategy")
public class StopLossPercentStrategy extends BaseStrategy{
  public StopLossPercentStrategy(PriceDao priceDao) {
    super(priceDao);
  }
  BasePrice pricePrev;
  double stopLossPercent = .97;
  @Override
  public Signal getSignal(BasePrice price) {
    if (pricePrev == null) {
      pricePrev = price;
      return null;
    }
    if ((price.getLow() - pricePrev.getLow()) < stopLossPercent*pricePrev.getLow()) {
      pricePrev = price;
      return Utils.createSignal(price, SignalType.LongClose);
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
