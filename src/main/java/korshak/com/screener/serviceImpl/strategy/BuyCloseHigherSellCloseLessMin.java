package korshak.com.screener.serviceImpl.strategy;

import java.util.Map;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.OptParam;
import korshak.com.screener.dao.OptParamDao;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("BuyCloseHigherSellCloseLessMin")
public class BuyCloseHigherSellCloseLessMin extends BaseStrategy {
  BasePrice pricePrev;

  public BuyCloseHigherSellCloseLessMin(PriceDao priceDao, OptParamDao optParamDao) {
    super(priceDao, optParamDao);
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
    if (price.getClose() > priceOfBackupTimeframe.getClose()) {
      signal = Utils.createSignal(price, SignalType.LongOpen,
          "close > " + priceOfBackupTimeframe.getClose());
    }
    if (price.getClose() < priceOfBackupTimeframe.getLow()) {
      signal = Utils.createSignal(price, SignalType.ShortOpen,
          "close<" + priceOfBackupTimeframe.getClose());
    }
    return signal;
  }

  @Override
  public void setOptParams(Map<String, OptParam> optParamsMap) {

  }
}

