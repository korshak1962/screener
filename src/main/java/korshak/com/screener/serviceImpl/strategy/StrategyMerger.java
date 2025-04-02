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
import korshak.com.screener.vo.SubStrategy;
import org.springframework.stereotype.Service;

@Service("StrategyMerger")
public class StrategyMerger implements Strategy {
  public static final String STOP_LOSS_PERCENT = "StopLoss";
  public static final String START_DATE = "startDate";
  public static final String END_DATE = "endDate";
  final PriceDao priceDao;
  double stopLossMaxPercent;
  List<SubStrategy> subStrategies = new ArrayList<>();
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
    for (SubStrategy subStrategy : subStrategies) {
      subStrategy.getStrategy().init(ticker, subStrategy.getTimeFrame(), startDate, endDate);
    }
    return this;
  }

  @Override
  public String getStrategyName() {
    return "StrategyMerger " +
        subStrategies.stream().reduce(" ",
            (s1, s2) -> s1.getClass().getSimpleName() + " " + s2.getClass().getSimpleName(),
            String::concat);
  }

  @Override
  public List<? extends BasePrice> getPrices() {
    return this.prices;
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getIndicators() {
    if (this.subStrategies.iterator().next().getStrategy().getIndicators() != null) {
      return this.subStrategies.iterator().next().getStrategy().getIndicators();
    }
    return Map.of();
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators() {
    if (this.subStrategies.isEmpty()) {
      return Map.of();
    }
    if (this.subStrategies.iterator().next().getStrategy().getPriceIndicators() != null) {
      return this.subStrategies.iterator().next().getStrategy().getPriceIndicators();
    }
    return Map.of();
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

  public StrategyMerger addStrategy(Strategy strategy, TimeFrame timeFrame) {
    subStrategies.add(new SubStrategy(strategy, timeFrame));
    return this;
  }

  public StrategyMerger addStrategies(List<SubStrategy> subStrategies) {
    this.subStrategies.addAll(subStrategies);
    return this;
  }

  public List<SubStrategy> getSubStrategies() {
    return subStrategies;
  }

  public void mergeSignals() {
    dateToSignals = new HashMap<>();
    signalsLong = new ArrayList<>();
    for (SubStrategy subStrategy : subStrategies) {
      subStrategy.getStrategy().calcSignals();
      List<? extends Signal> signalsOfStrategy = subStrategy.getStrategy().getAllSignals(timeFrame);
      if (signalsOfStrategy.isEmpty()) {
        throw new RuntimeException(
            "Strategy " + subStrategy.getStrategy().getStrategyName() + " has no signals");
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
    //  System.out.println("stopLossMaxPercent = " + stopLossMaxPercent);
    this.stopLossMaxPercent = stopLossMaxPercent;
    return this;
  }

  public void initOptParams(Map<String, OptParam> mameToValue) {
    List<OptParam> optParams = new ArrayList<>();
    double initStopLoss = .8;
    optParams.add(
        new OptParam(ticker, STOP_LOSS_PERCENT, this.getClass().getSimpleName(), timeFrame,
            initStopLoss, "", .8f, .95f, 0.05f)
    );

    this.setStopLossPercent(initStopLoss);
    /*
    optParams.add(new OptParam(ticker, "startDate", this.getClass().getSimpleName(), timeFrame,
        0d, startDate.toString(), 0f, 0f, 1f));
    optParams.add(new OptParam(ticker, "endDate", this.getClass().getSimpleName(), timeFrame,
        0d, endDate.toString(), 0f, 0f, 1f));

     */
    optParamsMap = Utils.getOptParamsAsMap(optParams);
    if (mameToValue != null) {
      optParamsMap.putAll(mameToValue);
      setOptParams(optParamsMap);
    }
  }

  public Map<String, OptParam> getOptParams() {
    return optParamsMap;
  }

  @Override
  public void setOptParams(Map<String, OptParam> optParamsMap) {
    if (optParamsMap != null && optParamsMap.get(STOP_LOSS_PERCENT) != null) {
      this.setStopLossPercent(optParamsMap.get(STOP_LOSS_PERCENT).getValue());
    } else {
      throw new RuntimeException("No opt params for strategy = " + this.getClass().getSimpleName() +
          " ticker = " + ticker + " timeframe = " + timeFrame);
    }
  }
}
