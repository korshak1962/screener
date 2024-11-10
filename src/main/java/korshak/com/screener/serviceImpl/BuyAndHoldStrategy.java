package korshak.com.screener.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.vo.Signal;
import org.springframework.stereotype.Service;

@Service("BuyAndHoldStrategy")
public class BuyAndHoldStrategy implements Strategy {
  String name;
  @Override
  public List<Signal> getTrades(List<? extends BasePrice> prices) {
    List<Signal> signals = new ArrayList<>();
    signals.add(new Signal(prices.get(0).getId().getDate(), prices.get(0).getClose(), 1, 1));
    signals.add(new Signal(prices.getLast().getId().getDate(), prices.getLast().getClose(), -1, 1));
    return signals;
  }
  @Override
  public String getName() {
    return "Buy and Hold";
  }
}
