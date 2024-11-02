package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.PriceDao;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.vo.StrategyResult;
import korshak.com.screener.vo.Trade;
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

    return getStrategyResult(strategy, prices);
  }

  @Override
  public StrategyResult calculateProfitAndDrawdown(Strategy strategy,
                                                   String ticker,
                                                   TimeFrame timeFrame) {

    List<? extends BasePrice> prices = priceDao.findAllByTicker(ticker, timeFrame);
    return getStrategyResult(strategy, prices);
  }

  private static StrategyResult getStrategyResult(Strategy strategy,
                                                  List<? extends BasePrice> prices) {
    double totalPnL = 0;
    double maxProfit = 0;
    double maxDrawdown = 0;
    double currentDrawdown = 0;

    List<Trade> trades = strategy.getTrades(prices);
    // Keep track of open positions
    Map<Double, Integer> openPositions = new HashMap<>();

    for (Trade trade : trades) {
      if (trade.getAction() == 1) { // Buy
        // Add to open positions
        openPositions.put(trade.getPrice(),
            openPositions.getOrDefault(trade.getPrice(), 0) + trade.getValue());
      } else if (trade.getAction() == -1) { // Sell
        // Calculate PnL for this trade
        double pnl = 0;
        int remainingToSell = trade.getValue();

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
          pnl += (trade.getPrice() - buyPrice) * sharesToSell;

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
    return new StrategyResult(prices, maxDrawdown, totalPnL, maxProfit, trades);
  }

}
