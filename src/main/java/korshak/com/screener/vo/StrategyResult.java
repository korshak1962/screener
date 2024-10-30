package korshak.com.screener.vo;

import java.util.List;
import korshak.com.screener.dao.BasePrice;

public class StrategyResult {
  List<? extends BasePrice> prices;
  double maxDrawdown =0;
  double totalPnL = 0;
  double maxProfit = 0;
  List<Trade> trades;

  public StrategyResult(List<? extends BasePrice> prices,
                        double maxDrawdown, double totalPnL,
                        double maxProfit, List<Trade> trades) {
    this.prices = prices;
    this.maxDrawdown = maxDrawdown;
    this.totalPnL = totalPnL;
    this.maxProfit = maxProfit;
    this.trades = trades;
  }

  public List<? extends BasePrice> getPrices() {
    return prices;
  }

  public double getMaxDrawdown() {
    return maxDrawdown;
  }

  public double getTotalPnL() {
    return totalPnL;
  }

  public double getMaxProfit() {
    return maxProfit;
  }

  public List<Trade> getTrades() {
    return trades;
  }

  @Override
  public String toString() {
    return "StrategyResult{" +
        "maxProfit=" + maxProfit +
        ", totalPnL=" + totalPnL +
        ", maxDrawdown=" + maxDrawdown +
        '}';
  }
}
