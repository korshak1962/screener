package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceMin5;
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

  public BaseStrategy(PriceDao priceDao) {
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
    return allSignals;
  }

  public abstract Signal getSignal(BasePrice price);

  public abstract Signal getSignal(BasePrice prevPrice, BasePrice price);

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

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getIndicators() {
    return Map.of();
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators() {
    return Map.of();
  }

  @Override
  public List<Signal> getAllSignals(TimeFrame signalTimeFrame) {
    if (signalTimeFrame.ordinal() == this.timeFrame.ordinal()) {
      return getAllSignals();
    }
    if (signalTimeFrame.ordinal() > this.timeFrame.ordinal()) {
      throw new RuntimeException("Special timeframe " + signalTimeFrame +
          " must be smaller than strategy timeframe " + this.timeFrame);
    }
    List<Signal> specialTimeframeSignals = new ArrayList<>();
    List<BasePrice> signalPrices = getPricesForTimeframe(signalTimeFrame);
    if (signalPrices.isEmpty()) {
      return specialTimeframeSignals;
    }
    Iterator<? extends BasePrice> priceIterator = signalPrices.iterator();
    BasePrice currentPrice = priceIterator.hasNext() ? priceIterator.next() : null;
    BasePrice priceOfBackupTimeframe = priceIterator.hasNext() ? priceIterator.next() : null;
    BasePrice nextPriceOfBackupTimeframe = priceIterator.hasNext() ? priceIterator.next() : null;
    BasePrice aggregatedPrice = currentPrice;

    while (currentPrice != null &&
        currentPrice.getId().getDate().isBefore(priceOfBackupTimeframe.getId().getDate())) {
      currentPrice = priceIterator.hasNext() ? priceIterator.next() : null;
    }
    while (currentPrice != null) {
      if (nextPriceOfBackupTimeframe != null
          &&
          !currentPrice.getId().getDate().isBefore(nextPriceOfBackupTimeframe.getId().getDate())) {
        aggregatedPrice = currentPrice;
        priceOfBackupTimeframe = nextPriceOfBackupTimeframe;
        nextPriceOfBackupTimeframe = priceIterator.hasNext() ? priceIterator.next() : null;
      } else {
        aggregatedPrice = aggregatePrices(aggregatedPrice, currentPrice);
      }
      Signal specialSignal = getSignal(priceOfBackupTimeframe, currentPrice);
      if (specialSignal != null) {
        specialTimeframeSignals.add(specialSignal);
      }
      currentPrice = priceIterator.hasNext() ? priceIterator.next() : null;
    }
    return specialTimeframeSignals;
  }

  List<BasePrice> getPricesForTimeframe(TimeFrame specialTimeFrame) {
    // Get prices for the special timeframe
    List<? extends BasePrice> specialPrices = priceDao.findByDateRange(
        ticker,
        prices.getFirst().getId().getDate(),
        endDate,
        specialTimeFrame
    );
    return (List<BasePrice>) specialPrices;
  }

  static BasePrice getPriceOfSignalDate(Iterator<? extends BasePrice> priceIterator,
                                        Signal currentSignal) {
    // Get to the first price that's not before current signal
    BasePrice currentPrice = null;
    while (priceIterator.hasNext()) {
      currentPrice = priceIterator.next();
      if (!currentPrice.getId().getDate().isBefore(currentSignal.getDate())) {
        break;
      }
    }
    return currentPrice;
  }

  private BasePrice aggregatePrices(BasePrice existing, BasePrice newPrice) {
    PriceMin5 aggregated = new PriceMin5();
    aggregated.setId(newPrice.getId());
    aggregated.setOpen(existing.getOpen());
    aggregated.setHigh(Math.max(existing.getHigh(), newPrice.getHigh()));
    aggregated.setLow(Math.min(existing.getLow(), newPrice.getLow()));
    aggregated.setClose(newPrice.getClose());
    aggregated.setVolume(existing.getVolume() + newPrice.getVolume());
    return aggregated;
  }
}
