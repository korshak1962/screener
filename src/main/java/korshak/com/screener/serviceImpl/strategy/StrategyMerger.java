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
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("StrategyMerger")
public class StrategyMerger implements Strategy {

  double stopLossMaxPercent = .97;

  Map<String, Strategy> nameToStrategy = new HashMap<>();
  TimeFrame timeFrame;
  LocalDateTime startDate;
  LocalDateTime endDate;
  final PriceDao priceDao;
  String ticker;
  List<? extends BasePrice> prices;
  List<Signal> signalsLong = new ArrayList<>();
  List<Signal> signalsShort = new ArrayList<>();

  Map<LocalDateTime, List<Signal>> dateToSignals = new HashMap<>();

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
    return this;
  }

  @Override
  public String StrategyName() {
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

  public StrategyMerger addStrategy(Strategy strategy) {
    nameToStrategy.put(strategy.StrategyName(), strategy);
    List<? extends Signal> signalsOfStrategy = strategy.getAllSignals(timeFrame);
    if (signalsOfStrategy.isEmpty()) {
      throw new RuntimeException("Strategy " + strategy.StrategyName() + " has no signals");
    }
    signalsOfStrategy.forEach(signal -> {
      if (!dateToSignals.containsKey(signal.getDate())) {
        dateToSignals.put(signal.getDate(), new ArrayList<>());
      } else {
        dateToSignals.get(signal.getDate()).add(signal);
      }
    });
    return this;
  }

  public Map<String, Strategy> getNameToStrategy() {
    return nameToStrategy;
  }

  public void mergeSignals() {
    Signal lastSignal = null;
    for (BasePrice price : prices) {
      // stopLoss
      if (lastSignal != null && lastSignal.getSignalType() == SignalType.LongOpen &&
          price.getLow() < stopLossMaxPercent * lastSignal.getPrice()) {
        Signal signalStopLoss = Utils.createSignal(price, SignalType.LongClose);
        lastSignal =
            Utils.fillLongShortLists(signalStopLoss, lastSignal, signalsShort, signalsLong);
        continue;
      }

      List<Signal> signalsForPrice = dateToSignals.get(price.getId().getDate());
      //  decision make
      if (signalsForPrice == null || signalsForPrice.isEmpty()) {
        continue;
      }
      Signal signalMin = Collections.min(signalsForPrice,
          Comparator.comparingInt(sgnal -> sgnal.getSignalType().value));
      lastSignal = Utils.fillLongShortLists(signalMin, lastSignal, signalsShort, signalsLong);
    }
  }

  public double getStopLossMaxPercent() {
    return stopLossMaxPercent;
  }

  public void setStopLossMaxPercent(double stopLossMaxPercent) {
    this.stopLossMaxPercent = stopLossMaxPercent;
  }
}
