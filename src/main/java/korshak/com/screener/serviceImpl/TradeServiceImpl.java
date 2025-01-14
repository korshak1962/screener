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
    double longPnL = 0;
    TreeMap<LocalDateTime, Double> currentPnL = new TreeMap<>();
    Map<LocalDateTime, Double> minLongPnl = new HashMap<>();
    List<Trade> tradesLong = new ArrayList<>();
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

    while (iteratorSignal.hasNext()) {
      Signal currentSignal = iteratorSignal.next();
      if (currentSignal.getSignalType() == SignalType.LongClose) {
        Trade trade = new Trade(prevSignal, currentSignal);
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

    double maxPossibleLoss = calcMaxPossibleLoss(strategy.getPrices());
    indicators.put("long PnL", currentPnL);
    return new StrategyResult(strategy.getPrices(), longPnL, 0,
        longPnL, minLongPnl, Map.of(), tradesLong,
        List.of(), strategy.getSignalsLong(), maxPossibleLoss, indicators);
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
    double maxPossibleLoss = calcMaxPossibleLoss(strategy.getPrices());
    indicators.put("current PnL", currentPnL);
    return new StrategyResult(strategy.getPrices(), 0, shortPnL,
        shortPnL, Map.of(), minShortPnl, List.of(),
        tradesShort, strategy.getSignalsShort(), maxPossibleLoss, indicators);
  }

  double calcMaxPossibleLoss(List<? extends BasePrice> prices) {
    BasePrice minPrice = prices.stream().min(Comparator.comparing(BasePrice::getClose)).get();
    return minPrice.getClose() - prices.getFirst().getClose();
  }
}
