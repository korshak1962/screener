package korshak.com.screener.vo;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;

public class StrategyResult {
  private final List<? extends BasePrice> prices;
  private final double longPnL;
  private double shortPnL;
  private double buyAndHoldPnL;
  private final double totalPnL;
  private final double maxPossibleLoss;
  private final Map<LocalDateTime, Double> minLongPnl; // the minimum profit or max loss
  private final Map<LocalDateTime, Double> worstLongTrade;
  private final Map<LocalDateTime, Double> minShortPnl;
  private final List<Trade> tradesLong;
  private final List<Trade> tradesShort;
  private final List<? extends Signal> signals;
  private final Map<String, NavigableMap<LocalDateTime, Double>> indicators;
  private final Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators;
  DecimalFormat df = new DecimalFormat("#.##");
  private Map<String, Double> optParams;
  private int profitLongTradesQnty;
  private int lostLongTradesQnty;
  private double sumOfProfitTrades;
  private double sumOfLostTrades;
  private double profitToLostRatio;

  public StrategyResult(List<? extends BasePrice> prices, double longPnL, double shortPnL,
                        double totalPnL, Map<LocalDateTime, Double> minLongPnl,
                        Map<LocalDateTime, Double> minShortPnl, List<Trade> tradesLong,
                        List<Trade> tradesShort, List<? extends Signal> signals,
                        double maxPossibleLoss,
                        Map<String, NavigableMap<LocalDateTime, Double>> indicators,
                        Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators,
                        Map<LocalDateTime, Double> worstLongTrade) {
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
    this.priceIndicators = priceIndicators;
    this.worstLongTrade = worstLongTrade;
    calcProfitAndLostRationLong();
  }

  public String toExcelString() {
    return "PnL=" + df.format(longPnL) +
      //  ", sPnL=" + df.format(shortPnL) +
        ", BHPnL=" + df.format(buyAndHoldPnL) +
        ", P/L= " + df.format(profitToLostRatio);
  }

  @Override
  public String toString() {
    return prices.getFirst().getId().getTicker() +
        " \n lPnL=" + df.format(longPnL) +
        ", BHPnL=" + df.format(buyAndHoldPnL) +
         ", sPnL=" + df.format(shortPnL) +
        //  ", totalPnL=" + df.format(totalPnL) +
        ", pToLRatio=" + df.format(profitToLostRatio) +
        ", prLTrQnty=" + profitLongTradesQnty +
        ", lostLTrQnty=" + lostLongTradesQnty +
        //   ", totalPnL=" + df.format(totalPnL) +
        //  ", minLongPnl=" + minLongPnl +
        // ", worstLongTrade=" + worstLongTrade +
        //   ", minShortPnl=" + minShortPnl +
        ", AnPercentRetLong =" + df.format(getAnnualPercentageReturnLong())
        // ", maxPossibleLossIfBuyAndHold=" + df.format(maxPossibleLoss)
        ;
  }

  public double getAnnualPercentageReturnLong() {
    if (tradesLong.isEmpty()) {
      return 0.0;
    }
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

  public List<? extends Signal> getSignals() {
    return signals;
  }

  public double getMaxPossibleLoss() {
    return maxPossibleLoss;
  }

  public Map<String, NavigableMap<LocalDateTime, Double>> getIndicators() {
    return indicators;
  }

  public Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators() {
    return priceIndicators;
  }

  public Map<String, Double> getOptParams() {
    return optParams;
  }

  public void setOptParams(Map<String, Double> optParams) {
    this.optParams = optParams;
  }

  private void calcProfitAndLostRationLong() {
    for (Trade trade : tradesLong) {
      if (trade.getPnl() > 0) {
        profitLongTradesQnty++;
        sumOfProfitTrades += trade.getPnl();
      } else {
        lostLongTradesQnty++;
        sumOfLostTrades -= trade.getPnl();
      }
    }
    if (sumOfLostTrades > 0) {
      profitToLostRatio = sumOfProfitTrades / sumOfLostTrades;
    }
  }

  public void setShortPnL(double shortPnL) {
    this.shortPnL = shortPnL;
  }

  public double getBuyAndHoldPnL() {
    return buyAndHoldPnL;
  }

  public void setBuyAndHoldPnL(double buyAndHoldPnL) {
    this.buyAndHoldPnL = buyAndHoldPnL;
  }
}
