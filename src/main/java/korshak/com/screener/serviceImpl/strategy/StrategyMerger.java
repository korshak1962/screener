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
import korshak.com.screener.dao.Param;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Configurable;
import korshak.com.screener.service.strategy.PostTradeStrategy;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import org.springframework.stereotype.Service;

@Service("StrategyMerger")
public class StrategyMerger {
  public static final String START_DATE = "startDate";
  public static final String END_DATE = "endDate";
  final PriceDao priceDao;
  List<Strategy> subStrategies = new ArrayList<>();
  List<PostTradeStrategy> postTradeStrategies = new ArrayList<>();
  TimeFrame timeFrame;
  LocalDateTime startDate;
  LocalDateTime endDate;
  String ticker;
  List<? extends BasePrice> prices;
  List<Signal> signalsLong;
  List<Signal> signalsShort = new ArrayList<>();
  Map<String, Param> paramsMap = new HashMap<>();
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


  public List<Signal> getSignalsLong() {
    if (signalsLong.isEmpty()) {
      mergeSignals();
    }
    return signalsLong;
  }


  public List<? extends Signal> getSignalsShort() {
    return signalsShort;
  }


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
    for (Strategy strategy : subStrategies) {
      strategy.init(ticker, strategy.getTimeFrame(), startDate, endDate);
    }
    return this;
  }


  public String getStrategyName() {
    return "StrategyMerger " +
        subStrategies.stream().reduce(" ",
            (s1, s2) -> s1.getClass().getSimpleName() + " " + s2.getClass().getSimpleName(),
            String::concat);
  }


  public List<? extends BasePrice> getPrices() {
    return this.prices;
  }


  public Map<String, NavigableMap<LocalDateTime, Double>> getIndicators() {
    if (this.subStrategies.iterator().next().getIndicators() != null) {
      return this.subStrategies.iterator().next().getIndicators();
    }
    return Map.of();
  }


  public Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators() {
    Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators = new HashMap<>();
    for(Strategy strategy : subStrategies){
      priceIndicators.putAll(strategy.getPriceIndicators());
    }
    return priceIndicators;
  }


  public TimeFrame getTimeFrame() {
    return timeFrame;
  }


  public String getTicker() {
    return ticker;
  }


  public LocalDateTime getStartDate() {
    return startDate;
  }


  public LocalDateTime getEndDate() {
    return endDate;
  }

  public StrategyMerger addStrategy(Strategy strategy) {
    subStrategies.add(strategy);
    return this;
  }

  public StrategyMerger addStrategies(List<Strategy> subStrategies) {
    this.subStrategies.addAll(subStrategies);
    return this;
  }

  public List<Strategy> getSubStrategies() {
    return subStrategies;
  }

  public void mergeSignals() {
    dateToSignals = new HashMap<>();
    signalsLong = new ArrayList<>();
    signalsShort = new ArrayList<>();
    for (Strategy subStrategy : subStrategies) {
      subStrategy.calcSignals();
      List<? extends Signal> signalsOfStrategy = subStrategy.getAllSignals(timeFrame);
      if (signalsOfStrategy.isEmpty()) {
       System.out.println("Strategy " + subStrategy.getStrategyName() + " has no signals");
           return;
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
      List<Signal> signalsForPrice = getSignalsWithPostTrade(price, lastSignal);
      if (signalsForPrice == null || signalsForPrice.isEmpty()) {
        continue;
      }
      Signal signalMin = Collections.min(signalsForPrice, signalComparator);
      lastSignal = Utils.fillLongShortLists(signalMin, lastSignal, signalsShort, signalsLong);
    }
  }

  private List<Signal> getSignalsWithPostTrade(BasePrice price, Signal lastSignal) {
    List<Signal> postTradeSignals = getPostTradeSignals(price, lastSignal);
    List<Signal> signalsForPrice = dateToSignals.get(price.getId().getDate());
    if (!postTradeSignals.isEmpty()) {
      if (signalsForPrice == null) {
        signalsForPrice = new ArrayList<>();
      }
      signalsForPrice.addAll(postTradeSignals);
    }
    return signalsForPrice;
  }

  private List<Signal> getPostTradeSignals(BasePrice price, Signal lastSignal) {
    List<Signal> postTradeSignals = new ArrayList<>();
    for (PostTradeStrategy strategy : postTradeStrategies){
      Signal postTradeSignal = strategy.getPostTradeSignal(price, lastSignal);
      if (postTradeSignal != null) {
        postTradeSignals.add(postTradeSignal);
      }
    }
    return postTradeSignals;
  }

  List<Configurable> getConfigurables(){
    List<Configurable> configurables = new ArrayList<>();
    for (Strategy strategy : subStrategies) {
      if (strategy instanceof Configurable) {
        configurables.add(strategy);
      }
    }
    for (PostTradeStrategy strategy : postTradeStrategies) {
      if (strategy instanceof Configurable) {
        configurables.add(strategy);
      }
    }
    return configurables;
  }

  public List<PostTradeStrategy> getPostTradeStrategies() {
    return postTradeStrategies;
  }
}
