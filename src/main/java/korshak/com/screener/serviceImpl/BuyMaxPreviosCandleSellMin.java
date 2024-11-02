package korshak.com.screener.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.vo.Trade;
import org.springframework.stereotype.Service;

@Service("BuyMaxPreviosCandleSellMin")
public class BuyMaxPreviosCandleSellMin implements Strategy {
  @Override
  public List<Trade> getTrades(List<? extends BasePrice> prices) {
    List<Trade> trades = new ArrayList<>();
    double prevHigh = prices.getFirst().getHigh();
    return trades;
  }

  @Override
  public String getName() {
    return "BuyMaxPreviosCandleSellMin";
  }
}
