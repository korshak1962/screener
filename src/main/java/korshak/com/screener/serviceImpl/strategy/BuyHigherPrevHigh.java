package korshak.com.screener.serviceImpl.strategy;

import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("BuyHigherPrevHigh")
public class BuyHigherPrevHigh extends BaseStrategy {

  BasePrice pricePrev;

  public BuyHigherPrevHigh(PriceDao priceDao) {
    super(priceDao);
  }

  @Override
  public Signal getSignal(BasePrice price) {
    if (pricePrev == null) {
      pricePrev = price;
      return null;
    }
    Signal signal = getSignal(pricePrev, price);
    pricePrev = price;
    return signal;
  }

  @Override
  public Signal getSignal(BasePrice priceOfBackupTimeframe, BasePrice price) {
    Signal signal = null;
    if (price.getHigh() > priceOfBackupTimeframe.getHigh()) {
      signal = Utils.createSignal(price, SignalType.LongOpen);
    }
    if (price.getLow() < priceOfBackupTimeframe.getLow()) {
      signal = Utils.createSignal(price, SignalType.LongClose);
    }
    return signal;
  }
}
