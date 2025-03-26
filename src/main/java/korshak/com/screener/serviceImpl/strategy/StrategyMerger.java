package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.OptParam;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("StrategyMerger")
public class StrategyMerger implements Strategy {

  final PriceDao priceDao;
  double stopLossMaxPercent = .98;
  Map<String, Strategy> nameToStrategy = new HashMap<>();
  TimeFrame timeFrame;
  LocalDateTime startDate;
  LocalDateTime endDate;
  String ticker;
  List<? extends BasePrice> prices;
  List<Signal> signalsLong;
  List<Signal> signalsShort = new ArrayList<>();
  Map<String, OptParam> optParamsMap = new HashMap<>();
  Map<LocalDateTime, List<Signal>> dateToSignals;
  Comparator<Signal> signalComparator = (s1, s2) -> {
    // First compare by SignalType value
    int valueCompare = Integer.compare(s1.getSignalType().value, s2.getSignalType().value);
    if (valueCompare != 0) {
      return valueCompare;
    }
    // If SignalType values are equal, compare by price in reverse order (to get max price)
    return Double.compare(s2.getPrice(), s1.getPrice());
  };

  public StrategyMerger(PriceDao priceDao) {
    this.priceDao = priceDao;
  }

  @Override
  public List<? extends Signal> getSignalsLong() {
    if (signalsLong.isEmpty()) {
      mergeSignals();
    }
    return signalsLong;
  }

  @Override
  public List<? extends Signal> getSignalsShort() {
    return signalsShort;
  }

  @Override
  public StrategyMerger init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
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
    initOptParams(null);
    return this;
  }

  @Override
  public String getStrategyName() {
    return "StrategyMerger " +
        nameToStrategy.keySet().stream().reduce("", (s1, s2) -> s1 + " " + s2);
  }

  @Override
  public List<? extends BasePrice> getPrices() {
    return this.prices;
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getIndicators() {
    return this.nameToStrategy.values().iterator().next().getIndicators();
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators() {
    return this.nameToStrategy.values().iterator().next().getPriceIndicators();
  }

  @Override
  public TimeFrame getTimeFrame() {
    return timeFrame;
  }

  @Override
  public String getTicker() {
    return ticker;
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
  public List<Signal> getAllSignals() {
    if (signalsLong.isEmpty()) {
      mergeSignals();
    }
    return dateToSignals.values().stream()
        .flatMap(List::stream)
        .toList();
  }

  @Override
  public List<Signal> getAllSignals(TimeFrame timeFrame) {
    return List.of();
  }

  @Override
  public void calcSignals() {

  }

  public StrategyMerger addStrategy(Strategy strategy) {
    nameToStrategy.put(strategy.getStrategyName() + strategy.getTimeFrame()+strategy.hashCode(), strategy);
    return this;
  }

  public Map<String, Strategy> getNameToStrategy() {
    return nameToStrategy;
  }

  public void mergeSignals() {
    dateToSignals = new HashMap<>();
    signalsLong = new ArrayList<>();
    for (Strategy strategy : nameToStrategy.values()) {
      strategy.calcSignals();
      List<? extends Signal> signalsOfStrategy = strategy.getAllSignals(timeFrame);
      if (signalsOfStrategy.isEmpty()) {
        throw new RuntimeException("Strategy " + strategy.getStrategyName() + " has no signals");
      }
      signalsOfStrategy.forEach(signal -> {
        if (!dateToSignals.containsKey(signal.getDate())) {
          dateToSignals.put(signal.getDate(), new ArrayList<>());
        }
        dateToSignals.get(signal.getDate()).add(signal);
      });
    }
    Signal lastSignal = null;
    for (BasePrice price : prices) {
      List<Signal> signalsForPrice = getSignalsWithStopLoss(price, lastSignal);
      if (signalsForPrice == null || signalsForPrice.isEmpty()) {
        continue;
      }
      Signal signalMin = Collections.min(signalsForPrice, signalComparator);
      lastSignal = Utils.fillLongShortLists(signalMin, lastSignal, signalsShort, signalsLong);
    }
  }

  private List<Signal> getSignalsWithStopLoss(BasePrice price, Signal lastSignal) {
    // stopLoss
    Signal signalStopLoss = null;
    if (lastSignal != null && lastSignal.getSignalType() == SignalType.LongOpen &&
        price.getLow() < stopLossMaxPercent * lastSignal.getPrice()) {
      signalStopLoss = Utils.createSignal(price, SignalType.LongClose,
          stopLossMaxPercent * lastSignal.getPrice(), "stop loss");
    }
    List<Signal> signalsForPrice = dateToSignals.get(price.getId().getDate());
    if (signalStopLoss != null) {
      if (signalsForPrice == null) {
        signalsForPrice = new ArrayList<>();
      }
      signalsForPrice.add(signalStopLoss);
    }
    return signalsForPrice;
  }

  public double getStopLossMaxPercent() {
    return stopLossMaxPercent;
  }

  public StrategyMerger setStopLossPercent(double stopLossMaxPercent) {
    this.stopLossMaxPercent = stopLossMaxPercent;
    return this;
  }
  public void initOptParams(Map<String, OptParam> mameToValue) {
    List<OptParam> optParams = new ArrayList<>();
    optParams.add(
        new OptParam(ticker, "stopLossPercent", this.getClass().getSimpleName(), timeFrame,
            .98, "", .92f, 0.99f, 0.01f)
    );
    /*
    optParams.add(new OptParam(ticker, "startDate", this.getClass().getSimpleName(), timeFrame,
        0d, startDate.toString(), 0f, 0f, 1f));
    optParams.add(new OptParam(ticker, "endDate", this.getClass().getSimpleName(), timeFrame,
        0d, endDate.toString(), 0f, 0f, 1f));

     */
    optParamsMap = Utils.getOptParamsAsMap(optParams);
    if (mameToValue != null) {
      optParamsMap.putAll(mameToValue);
    }
  }

 public Map<String, OptParam> getOptParams() {
    return optParamsMap;
  }
}
