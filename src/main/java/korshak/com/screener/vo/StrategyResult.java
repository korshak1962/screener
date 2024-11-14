package korshak.com.screener.vo;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import korshak.com.screener.dao.BasePrice;

public class StrategyResult {
  private final List<? extends BasePrice> prices;
  private final double longPnL;
  private final double shortPnL;
  private final double totalPnL;
  private final double maxPossibleLoss;
  private final Map<LocalDateTime, Double> minLongPnl;
  private final Map<LocalDateTime, Double> minShortPnl;
  private final List<Trade> tradesLong;
  private final List<Trade> tradesShort;
  private final List<Signal> signals;
  private Map<String,TreeMap<LocalDateTime,Double>> indicators;


  public StrategyResult(List<? extends BasePrice> prices, double longPnL, double shortPnL,
                        double totalPnL, Map<LocalDateTime, Double> minLongPnl,
                        Map<LocalDateTime, Double> minShortPnl, List<Trade> tradesLong,
                        List<Trade> tradesShort, List<Signal> signals, double maxPossibleLoss,
                        Map<String,TreeMap<LocalDateTime,Double>> indicators) {
    this.prices = prices;
    this.longPnL = longPnL;
    this.shortPnL = shortPnL;
    this.totalPnL = totalPnL;
    this.maxPossibleLoss = maxPossibleLoss;
    this.minLongPnl = minLongPnl;
    this.minShortPnl = minShortPnl;
    this.tradesLong = tradesLong;
    this.tradesShort = tradesShort;
    this.signals = signals;
    this.indicators = indicators;
  }

  @Override
  public String toString() {
    return "StrategyResult{" +
        "  longPnL=" + longPnL +
        ", shortPnL=" + shortPnL +
        ", totalPnL=" + totalPnL +

        ", minLongPnl=" + minLongPnl +
        ", minShortPnl=" + minShortPnl +
        ", AnnualPercentageReturnLong =" +getAnnualPercentageReturnLong() +
        ", maxPossibleLossIfBuyAndHold=" + maxPossibleLoss +
        '}';
  }

  public double getAnnualPercentageReturnLong() {
    long days = ChronoUnit.DAYS.between(prices.get(0).getId().getDate(),
        prices.getLast().getId().getDate());
    double profitPercent = longPnL / tradesLong.getFirst().getOpen().getPrice();
    double years = 365.0 / days;
    return (profitPercent * years) * 100;
  }

  public List<? extends BasePrice> getPrices() {
    return prices;
  }

  public double getLongPnL() {
    return longPnL;
  }

  public double getShortPnL() {
    return shortPnL;
  }

  public double getTotalPnL() {
    return totalPnL;
  }

  public Map<LocalDateTime, Double> getMinLongPnl() {
    return minLongPnl;
  }

  public Map<LocalDateTime, Double> getMinShortPnl() {
    return minShortPnl;
  }

  public List<Trade> getTradesLong() {
    return tradesLong;
  }

  public List<Trade> getTradesShort() {
    return tradesShort;
  }

  public List<Signal> getSignals() {
    return signals;
  }

  public double getMaxPossibleLoss() {
    return maxPossibleLoss;
  }

  public Map<String, TreeMap<LocalDateTime, Double>> getIndicators() {
    return indicators;
  }
}
