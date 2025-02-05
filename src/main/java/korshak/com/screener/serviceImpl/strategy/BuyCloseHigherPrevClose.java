package korshak.com.screener.serviceImpl.strategy;

import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("BuyCloseHigherPrevClose")
public class BuyCloseHigherPrevClose extends BaseStrategy {
  BasePrice pricePrev;

  public BuyCloseHigherPrevClose(PriceDao priceDao) {
    super(priceDao);
  }

  @Override
  public Signal getSignal(BasePrice price) {
    if (pricePrev == null) {
      pricePrev = price;
      return null;
    }
    Signal signal = getSignal(pricePrev,price);
    pricePrev = price;
    return signal;
  }

  @Override
   public Signal getSignal(BasePrice prevPrice,BasePrice price) {
    Signal signal = null;
    if (price.getClose() > prevPrice.getClose()) {
      signal = Utils.createSignal(price, SignalType.LongOpen);
    } else if (price.getClose() < prevPrice.getClose()) {
      signal = Utils.createSignal(price, SignalType.ShortOpen);
    }
    return signal;
  }
}
