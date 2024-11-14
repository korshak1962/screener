package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.PriceDao;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.StrategyResult;
import korshak.com.screener.vo.Trade;
import org.springframework.stereotype.Service;

@Service
public class TradeServiceImpl implements TradeService {
  Map<String, TreeMap<LocalDateTime, Double>> indicators = new HashMap<>();
  private final PriceDao priceDao;

  public TradeServiceImpl(PriceDao priceDao) {
    this.priceDao = priceDao;
  }


  @Override
  /**
   * Calculate profit/loss and maximum drawdown from a list of trades
   * @param trades List of trades in chronological order
   * @return double[]{profit/loss, maxDrawdown}
   */
  public StrategyResult calculateProfitAndDrawdown(Strategy strategy,
                                                   String ticker,
                                                   LocalDateTime startDate,
                                                   LocalDateTime endDate,
                                                   TimeFrame timeFrame) {

    List<? extends BasePrice> prices = priceDao.findByDateRange(ticker,
        startDate,
        endDate,
        timeFrame);

    return getStrategyResult(strategy.getTrades(prices), prices);
  }

  @Override
  public StrategyResult calculateProfitAndDrawdown(Strategy strategy,
                                                   String ticker,
                                                   TimeFrame timeFrame) {

    List<? extends BasePrice> prices = priceDao.findAllByTicker(ticker, timeFrame);
    return getStrategyResult(strategy.getTrades(prices), prices);
  }

  private StrategyResult getStrategyResult(List<Signal> signals,
                                           List<? extends BasePrice> prices) {
    double longPnL = 0;
    double shortPnL = 0;
    double totalPnL = 0;
    TreeMap<LocalDateTime, Double> currentPnL = new TreeMap<>();
    Map<LocalDateTime, Double> minLongPnl = new HashMap<>();
    Map<LocalDateTime, Double> minShortPnl = new HashMap<>();

    List<Trade> tradesLong = new ArrayList<>();
    List<Trade> tradesShort = new ArrayList<>();

    Map<LocalDateTime, Double> unrealizedDrawDownsPerTrade = new HashMap<>(signals.size());
    Iterator<Signal> iteratorSignal = signals.iterator();
    Signal prevSignal = iteratorSignal.next();
    Double prevPnl = null;
    while (iteratorSignal.hasNext()) {
      Signal currentSignal = iteratorSignal.next();
      Trade trade = new Trade(prevSignal, currentSignal);
      prevSignal = currentSignal;
      if (currentSignal.getAction() == -1) { // Buy was now sell
        tradesLong.add(trade);
        longPnL += trade.getPnl();
        prevPnl = currentPnL.lastEntry() == null ? 0 : currentPnL.lastEntry().getValue();
        currentPnL.put(trade.getClose().getDate(), prevPnl + trade.getPnl());
        if (minLongPnl.isEmpty() || minLongPnl.values().iterator().next() > longPnL) {
          minLongPnl.put(trade.getClose().getDate(), longPnL);
        }
      } else if (currentSignal.getAction() == 1) { // Sell was now buy
        tradesShort.add(trade);
        shortPnL += trade.getPnl();
        if (minShortPnl.isEmpty() || minShortPnl.values().iterator().next() > shortPnL) {
          minShortPnl.put(trade.getClose().getDate(), shortPnL);
        }
      }

    }
    totalPnL = longPnL + shortPnL;
    double maxPossibleLoss = calcMaxPossibleLoss(prices);
    indicators.put("current PnL", currentPnL);
    return new StrategyResult(prices, longPnL, shortPnL,
        totalPnL, minLongPnl, minShortPnl, tradesLong,
        tradesShort, signals, maxPossibleLoss, indicators);
  }

  double calcMaxPossibleLoss(List<? extends BasePrice> prices) {
    BasePrice minPrice = prices.stream().min(Comparator.comparing(BasePrice::getClose)).get();
    return minPrice.getClose() - prices.getFirst().getClose();
  }
}
