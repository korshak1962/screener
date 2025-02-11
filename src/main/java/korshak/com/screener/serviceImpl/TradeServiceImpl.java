package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalTilt;
import korshak.com.screener.vo.SignalType;
import korshak.com.screener.vo.StrategyResult;
import korshak.com.screener.vo.Trade;
import org.springframework.stereotype.Service;

@Service
public class TradeServiceImpl implements TradeService {
  Map<String, NavigableMap<LocalDateTime, Double>> indicators = new HashMap<>();

  List<Trade> tradesLong;

  @Override
  /**
   * Calculate profit/loss and maximum drawdown from a list of trades
   * @param trades List of trades in chronological order
   * @return double[]{profit/loss, maxDrawdown}
   */
  public StrategyResult calculateProfitAndDrawdownLong(Strategy strategy,
                                                       String ticker,
                                                       LocalDateTime startDate,
                                                       LocalDateTime endDate,
                                                       TimeFrame timeFrame) {
    strategy.init(ticker,timeFrame,startDate,endDate);
    return calculateProfitAndDrawdownLong(strategy);
  }

  @Override
  public StrategyResult calculateProfitAndDrawdownLong(Strategy strategy) {
    tradesLong = new ArrayList<>();
    double longPnL = 0;
    TreeMap<LocalDateTime, Double> currentPnL = new TreeMap<>();
    Map<LocalDateTime, Double> minLongPnl = new HashMap<>();
    //========================= temporary
    if (strategy.getSignalsLong().getLast().getSignalType() == SignalType.LongOpen){
      SignalTilt signal = new SignalTilt(strategy.getPrices().getLast().getId().getDate(),
          strategy.getPrices().getLast().getClose(),SignalType.LongClose,
          0, 0);
      ((List<SignalTilt>)strategy.getSignalsLong()).add(signal);
    }
    //====================
    Iterator<? extends Signal> iteratorSignal = strategy.getSignalsLong().iterator();
    Signal prevSignal = iteratorSignal.next();
    double prevPnl;
    Map<LocalDateTime, Double> worstLongTrade = new HashMap<>();
    worstLongTrade.put(strategy.getSignalsLong().getFirst().getDate(),Double.MIN_VALUE);
    while (iteratorSignal.hasNext()) {
      Signal currentSignal = iteratorSignal.next();
      if (currentSignal.getSignalType() == SignalType.LongClose) {
        Trade trade = new Trade(prevSignal, currentSignal);
        if (worstLongTrade.values().iterator().next() > trade.getPnl()) {
          worstLongTrade.clear();
          worstLongTrade.put(trade.getClose().getDate(), trade.getPnl());
        }
        tradesLong.add(trade);
        longPnL += trade.getPnl();
        currentPnL.put(trade.getClose().getDate(), longPnL);
        if (minLongPnl.isEmpty() || minLongPnl.values().iterator().next() > longPnL) {
          minLongPnl.clear();
          minLongPnl.put(trade.getClose().getDate(), longPnL);
        }
      }
      prevSignal = currentSignal;
    }

    double maxPossibleLoss = calcMaxPossibleLossLong();
    calculateMaxPainPercentages(tradesLong, strategy.getPrices());
    indicators.put("long PnL", currentPnL);
    return new StrategyResult(strategy.getPrices(), longPnL, 0,
        longPnL, minLongPnl, Map.of(), tradesLong,
        List.of(), strategy.getSignalsLong(), maxPossibleLoss, indicators,worstLongTrade);
  }

  @Override
  public StrategyResult calculateProfitAndDrawdownShort(Strategy strategy) {
    double shortPnL = 0;
    TreeMap<LocalDateTime, Double> currentPnL = new TreeMap<>();
    Map<LocalDateTime, Double> minShortPnl = new HashMap<>();
    List<Trade> tradesShort = new ArrayList<>();
    Iterator<? extends Signal> iteratorSignal = strategy.getSignalsShort().iterator();
    Signal prevSignal = null;
    if (iteratorSignal.hasNext()) {
      prevSignal = iteratorSignal.next();
    }
    double prevPnl;
    while (iteratorSignal.hasNext()) {
      Signal currentSignal = iteratorSignal.next();
      if (currentSignal.getSignalType() == SignalType.ShortClose) {
        Trade trade = new Trade(prevSignal, currentSignal);
        tradesShort.add(trade);
        shortPnL += trade.getPnl();
        currentPnL.put(trade.getClose().getDate(), shortPnL);
        if (minShortPnl.isEmpty() || minShortPnl.values().iterator().next() > shortPnL) {
          minShortPnl.clear();
          minShortPnl.put(trade.getClose().getDate(), shortPnL);
        }
      }
      prevSignal = currentSignal;
    }
    double maxPossibleLoss = calcMaxPossibleLossLong();
    indicators.put("current PnL", currentPnL);
    return new StrategyResult(strategy.getPrices(), 0, shortPnL,
        shortPnL, Map.of(), minShortPnl, List.of(),
        tradesShort, strategy.getSignalsShort(), maxPossibleLoss, indicators,Map.of());
  }

  double calcMaxPossibleLossLong() {
    double cumulativePnL = 0;
    double maxLoss = 0;
    for (Trade trade : tradesLong) {
      cumulativePnL += trade.getPnl();
      if (cumulativePnL < maxLoss) {
        maxLoss = cumulativePnL;
      }
    }
      return maxLoss;
  }
  /**
   * Calculates maximum pain percentage for each trade based on minimum price during trade duration
   * @param trades List of trades ordered by time
   * @param prices List of prices ordered by time
   */
  public static void calculateMaxPainPercentages(List<Trade> trades, List<? extends BasePrice> prices) {
    if (trades == null || trades.isEmpty() || prices == null || prices.isEmpty()) {
      return;
    }
    int priceIndex = 0;
    for (Trade trade : trades) {
      LocalDateTime tradeStartTime = trade.getOpen().getDate();
      LocalDateTime tradeEndTime = trade.getClose().getDate();
      double openPrice = trade.getOpen().getPrice();
      // Find start index for this trade's time range
      while (priceIndex < prices.size() &&
          prices.get(priceIndex).getId().getDate().isBefore(tradeStartTime)) {
        priceIndex++;
      }
      // Find minimum price during trade duration
      double minPrice = prices.get(priceIndex).getClose();
      int currentIndex = priceIndex;
      while (currentIndex < prices.size()) {
        BasePrice price = prices.get(currentIndex);
        LocalDateTime priceTime = price.getId().getDate();
        if (priceTime.isAfter(tradeEndTime)) {
          break;
        }
        minPrice = Math.min(minPrice, price.getLow());
        currentIndex++;
      }
        double maxPainPercent = ((openPrice - minPrice) / openPrice) * 100.0;
        trade.setMaxPainPercent(maxPainPercent);
    }
  }
}
