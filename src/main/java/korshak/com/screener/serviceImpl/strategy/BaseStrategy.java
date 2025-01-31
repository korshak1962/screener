package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.vo.Signal;

public abstract class BaseStrategy implements Strategy {
  TimeFrame timeFrame;
  LocalDateTime startDate;
  LocalDateTime endDate;
  final PriceDao priceDao;
  String ticker;
  List<? extends BasePrice> prices;
  List<Signal> signalsLong = new ArrayList<>();
  List<Signal> signalsShort = new ArrayList<>();
  List<Signal> allSignals = new ArrayList<>();

  public BaseStrategy( PriceDao priceDao) {
    this.priceDao = priceDao;
  }

  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.timeFrame = timeFrame;
    this.ticker = ticker;
    this.startDate = startDate;
    this.endDate = endDate;
    this.prices = priceDao.findByDateRange(
        ticker,
        startDate,
        endDate,
        timeFrame
    );
  }
  @Override
  public String StrategyName() {
    return this.getClass().getName();
  }

  public List<Signal> calcSignals() {
    if (prices == null || prices.isEmpty()) {
      throw new RuntimeException("Prices are not initialized");
    }
    // Iterate through prices and collect all signals
    for (BasePrice price : prices) {
      Signal signalToAdd = getSignal(price);
      if (signalToAdd != null) {
        allSignals.add(signalToAdd);
      }
    }
    return  allSignals;
  }

  public abstract Signal getSignal(BasePrice price);

  @Override
  public TimeFrame getTimeFrame() {
    return timeFrame;
  }

  @Override
  public LocalDateTime getStartDate() {
    return startDate;
  }

  @Override
  public LocalDateTime getEndDate() {
    return endDate;
  }

  @Override
  public String getTicker() {
    return ticker;
  }

  @Override
  public List<? extends BasePrice> getPrices() {
    return prices;
  }

  @Override
  public List<Signal> getSignalsLong() {
    return signalsLong;
  }

  @Override
  public List<Signal> getSignalsShort() {
    return signalsShort;
  }

  @Override
  public List<Signal> getAllSignals() {
    if (allSignals.isEmpty()) {
      calcSignals();
    }
    return allSignals;
  }
}
