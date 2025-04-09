package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.Param;
import korshak.com.screener.dao.OptParamDao;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.vo.Signal;

public abstract class BaseStrategy implements Strategy {
  final PriceDao priceDao;
  final OptParamDao optParamDao;
  TimeFrame timeFrame;
  LocalDateTime startDate;
  LocalDateTime endDate;
  String ticker;
  Map<String, Param> optParamsMap = new HashMap<>();
  List<? extends BasePrice> prices;
  List<Signal> signalsLong = new ArrayList<>();
  List<Signal> signalsShort = new ArrayList<>();
  List<Signal> allSignals = new ArrayList<>();

  public BaseStrategy(PriceDao priceDao, OptParamDao optParamDao) {
    this.priceDao = priceDao;
    this.optParamDao = optParamDao;
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

  @Override
  public Strategy init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
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
    return this;
  }

  @Override
  public String getStrategyName() {
    return this.getClass().getSimpleName();
  }

  public void calcSignals() {
    allSignals.clear();
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
  }

  public abstract Signal getSignal(BasePrice price);

  public abstract Signal getSignal(BasePrice priceOfBackupTimeframe, BasePrice price);

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

  public void configure(Map<String, Param> nameToParam) {
    this.optParamsMap = nameToParam;
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
    Iterator<? extends BasePrice> signalPriceIterator = signalPrices.iterator();
    BasePrice currentPrice = signalPriceIterator.hasNext() ? signalPriceIterator.next() : null;
    Iterator<? extends BasePrice> backupPriceIterator = prices.iterator();
    BasePrice priceOfBackupTimeframe =
        backupPriceIterator.hasNext() ? backupPriceIterator.next() : null;
    BasePrice nextPriceOfBackupTimeframe =
        backupPriceIterator.hasNext() ? backupPriceIterator.next() : null;
    BasePrice nextNextPriceOfBackupTimeframe =
        backupPriceIterator.hasNext() ? backupPriceIterator.next() : null;

    while (currentPrice != null &&
        currentPrice.getId().getDate().isBefore(nextPriceOfBackupTimeframe.getId().getDate())) {
      currentPrice = signalPriceIterator.hasNext() ? signalPriceIterator.next() : null;
    }
    while (currentPrice != null) {
      if (nextNextPriceOfBackupTimeframe != null
          &&
          !currentPrice.getId().getDate()
              .isBefore(nextNextPriceOfBackupTimeframe.getId().getDate())) {
        priceOfBackupTimeframe = nextPriceOfBackupTimeframe;
        nextPriceOfBackupTimeframe = nextNextPriceOfBackupTimeframe;
        nextNextPriceOfBackupTimeframe =
            backupPriceIterator.hasNext() ? backupPriceIterator.next() : null;
      }
      Signal specialSignal = getSignal(priceOfBackupTimeframe, currentPrice);
      if (specialSignal != null) {
        specialTimeframeSignals.add(specialSignal);
      }
      currentPrice = signalPriceIterator.hasNext() ? signalPriceIterator.next() : null;
    }
    return specialTimeframeSignals;
  }

  List<BasePrice> getPricesForTimeframe(TimeFrame signalTimeFrame) {
    // Get prices for the special timeframe
    List<? extends BasePrice> specialPrices = priceDao.findByDateRange(
        ticker,
        prices.getFirst().getId().getDate(),
        endDate,
        signalTimeFrame
    );
    return (List<BasePrice>) specialPrices;
  }

  public Map<String, Param> getParams() {
    return optParamsMap;
  }
}
