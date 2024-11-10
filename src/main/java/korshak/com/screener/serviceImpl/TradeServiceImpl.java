package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.PriceDao;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.vo.StrategyResult;
import korshak.com.screener.vo.Signal;
import org.springframework.stereotype.Service;

@Service
public class TradeServiceImpl implements TradeService {

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
    double totalPnL = 0;
    double maxProfit = 0;
    double maxDrawdown = 0;
    double currentDrawdown = 0;
    Signal signalBuy = null;
    Map<LocalDateTime, Double> unrealizedDrawDownsPerTrade = new HashMap<>(signals.size());
    // Keep track of open positions
    Map<Double, Integer> openPositions = new HashMap<>();

    for (Signal signal : signals) {

      if (signal.getAction() == 1) { // Buy
        signalBuy = signal;
        // Add to open positions
        openPositions.put(signal.getPrice(),
            openPositions.getOrDefault(signal.getPrice(), 0) + signal.getValue());

      } else if (signal.getAction() == -1) { // Sell
        BasePrice minPricePerTrade =
            caclMinPricePerTrade(signalBuy.getDate(), signal.getDate(), prices);
        unrealizedDrawDownsPerTrade.put(minPricePerTrade.getId().getDate(),
            minPricePerTrade.getClose() - signalBuy.getPrice());
        // Calculate PnL for this trade
        double pnl = 0;
        int remainingToSell = signal.getValue();

        // Sort open positions by price to implement FIFO
        List<Map.Entry<Double, Integer>> sortedPositions =
            new ArrayList<>(openPositions.entrySet());
        sortedPositions.sort(Map.Entry.comparingByKey());

        for (Map.Entry<Double, Integer> position : sortedPositions) {
          if (remainingToSell <= 0) {
            break;
          }

          double buyPrice = position.getKey();
          int availableShares = position.getValue();
          int sharesToSell = Math.min(remainingToSell, availableShares);

          // Calculate PnL for this portion
          pnl += (signal.getPrice() - buyPrice) * sharesToSell;

          // Update remaining shares to sell
          remainingToSell -= sharesToSell;

          // Update or remove the position
          if (sharesToSell == availableShares) {
            openPositions.remove(buyPrice);
          } else {
            openPositions.put(buyPrice, availableShares - sharesToSell);
          }
        }

        // Update total PnL and track maximum profit
        totalPnL += pnl;
        maxProfit = Math.max(maxProfit, totalPnL);

        // Calculate current drawdown and update maximum drawdown
        currentDrawdown = maxProfit - totalPnL;
        maxDrawdown = Math.max(maxDrawdown, currentDrawdown);
      }
    }
    return new StrategyResult(prices, maxDrawdown, totalPnL, maxProfit, signals,
        unrealizedDrawDownsPerTrade);
  }

  private BasePrice caclMinPricePerTrade(LocalDateTime buyDate, LocalDateTime sellDate,
                                         List<? extends BasePrice> prices) {
    int priceIndex = Collections.binarySearch(
        prices,
        new BasePrice() {
          @Override
          public PriceKey getId() {
            return new PriceKey(null, buyDate);
          }
        },
        Comparator.comparing(price -> price.getId().getDate())
    );
    BasePrice minPricePerTrade = prices.get(priceIndex);

    while (prices.get(++priceIndex).getId().getDate().isBefore(sellDate)) {
      minPricePerTrade =
          minPricePerTrade.getClose() > prices.get(priceIndex).getClose() ? prices.get(priceIndex) :
              minPricePerTrade;
    }

    return minPricePerTrade.getClose() > prices.get(priceIndex).getClose() ?
        prices.get(priceIndex) :
        minPricePerTrade;
  }
}
