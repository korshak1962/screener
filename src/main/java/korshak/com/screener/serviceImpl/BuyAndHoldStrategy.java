package korshak.com.screener.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.vo.Trade;
import org.springframework.stereotype.Service;

@Service
public class BuyAndHoldStrategy implements Strategy {
  String name;
  @Override
  public List<Trade> getTrades(List<? extends BasePrice> prices) {
    List<Trade> trades = new ArrayList<>();
    trades.add(new Trade(prices.get(0).getId().getDate(), prices.get(0).getClose(), 1, 1));
    trades.add(new Trade(prices.getLast().getId().getDate(), prices.getLast().getClose(), -1, 1));
    return trades;
  }
  @Override
  public String getName() {
    return "Buy and Hold";
  }
}
