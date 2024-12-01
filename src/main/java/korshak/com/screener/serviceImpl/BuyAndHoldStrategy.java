package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("BuyAndHoldStrategy")
public class BuyAndHoldStrategy implements Strategy {
  String name;
  List<Signal> signals = new ArrayList<>();
  @Override
  public List<Signal> getSignals(List<? extends BasePrice> prices) {
    signals.add(new Signal(prices.get(0).getId().getDate(), prices.get(0).getClose(), SignalType.LongOpen));
    signals.add(new Signal(prices.getLast().getId().getDate(), prices.getLast().getClose(), SignalType.LongClose));
    return signals;
  }

  @Override
  public List<Signal> getSignals() {
    return signals;
  }

  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {

  }

  @Override
  public String getName() {
    return "Buy and Hold";
  }
}
