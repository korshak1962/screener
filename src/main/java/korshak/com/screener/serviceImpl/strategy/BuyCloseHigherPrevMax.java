package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("BuyCloseHigherPrevMax")
public class BuyCloseHigherPrevMax extends BaseStrategy {

  public BuyCloseHigherPrevMax(PriceDao priceDao) {
    super(priceDao);
  }

  BasePrice pricePrev;

  @Override
  public Signal getSignal(BasePrice price) {
    if (pricePrev == null) {
      pricePrev = price;
      return null;
    }
    if (price.getClose() > pricePrev.getHigh()) {
      pricePrev = price;
      return Utils.createSignal(price, SignalType.LongOpen);
    } else if (price.getClose() < pricePrev.getLow()) {
      pricePrev = price;
      return Utils.createSignal(price, SignalType.ShortOpen);
    }
    return null;
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getIndicators() {
    return Map.of();
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators() {
    return Map.of();
  }
}
