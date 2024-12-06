package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.PriceDao;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("BuyAndHoldStrategy")
public class BuyAndHoldStrategy implements Strategy {
  PriceDao priceDao;
  private List<? extends BasePrice> prices;
  String name;
  List<Signal> signals = new ArrayList<>();

  public List<Signal> getSignalsLong(List<? extends BasePrice> prices) {
    return signals;
  }

  @Override
  public List<Signal> getSignalsLong(){
     return  getSignalsLong(prices);
  }

  public BuyAndHoldStrategy(PriceDao priceDao) {
    this.priceDao = priceDao;
  }

  @Override
  public List<? extends Signal> getSignalsShort() {
    return List.of();
  }

  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.prices = priceDao.findByDateRange(
        ticker,
        startDate,
        endDate,
        timeFrame
    );
    signals.add(new Signal(prices.getFirst().getId().getDate(), prices.getFirst().getClose(), SignalType.LongOpen));
    signals.add(new Signal(prices.getLast().getId().getDate(), prices.getLast().getClose(), SignalType.LongClose));
  }

  @Override
  public String getName() {
    return "Buy and Hold";
  }

  @Override
  public List<? extends BasePrice> getPrices() {
    return prices;
  }
}
