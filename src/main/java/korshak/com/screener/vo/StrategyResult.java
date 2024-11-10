package korshak.com.screener.vo;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import korshak.com.screener.dao.BasePrice;

public class StrategyResult {
  List<? extends BasePrice> prices;
  double maxDrawdown = 0;
  double totalPnL = 0;
  double maxProfit = 0;
  List<Signal> signals;
  Map<LocalDateTime, Double> unrealizedDrawDownsPerTrade;

  public StrategyResult(List<? extends BasePrice> prices,
                        double maxDrawdown, double totalPnL,
                        double maxProfit, List<Signal> signals,
                        Map<LocalDateTime, Double> unrealizedDrawDownsPerTrade) {
    this.prices = prices;
    this.maxDrawdown = maxDrawdown;
    this.totalPnL = totalPnL;
    this.maxProfit = maxProfit;
    this.signals = signals;
    this.unrealizedDrawDownsPerTrade = unrealizedDrawDownsPerTrade;
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

  public List<Signal> getTrades() {
    return signals;
  }

  @Override
  public String toString() {
    return "StrategyResult{" +
        "maxProfit= " + maxProfit +
        ", totalPnL= " + totalPnL +
        ", maxDrawdown= " + maxDrawdown +
        ", getMaxUnrealizedDrawDown()= " + getMaxUnrealizedDrawDown().getValue() + " at " +
        getMaxUnrealizedDrawDown().getKey() +
        ", getAnnualPercentageReturn()= " + getAnnualPercentageReturn() +
        '}';
  }

  public Map<LocalDateTime, Double> getUnrealizedDrawDownsPerTrade() {
    return unrealizedDrawDownsPerTrade;
  }

  public Map.Entry<LocalDateTime, Double> getMaxUnrealizedDrawDown() {
    return unrealizedDrawDownsPerTrade.entrySet().stream()
        .min(Map.Entry.comparingByValue()).get();
  }

  public double getAnnualPercentageReturn() {
    long days = ChronoUnit.DAYS.between(prices.get(0).getId().getDate(),
        prices.getLast().getId().getDate());
    double profitPercent = totalPnL / signals.getFirst().getPrice();
    double years = 365.0 / days;
    return (profitPercent * years) * 100;
  }
}
