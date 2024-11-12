package korshak.com.screener.serviceImpl.chart;

import korshak.com.screener.vo.Trade;
import org.jfree.data.xy.AbstractXYDataset;
import java.util.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TradeHistogramDataset extends AbstractXYDataset {
  private final List<Trade> trades;

  public TradeHistogramDataset(List<Trade> trades) {
    this.trades = trades;
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable getSeriesKey(int series) {
    return "Trade PnL";
  }

  @Override
  public int getItemCount(int series) {
    return trades.size();
  }

  private long toEpochMilli(LocalDateTime dateTime) {
    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  @Override
  public Number getX(int series, int item) {
    Trade trade = trades.get(item);
    long openTime = toEpochMilli(trade.getOpen().getDate());
    long closeTime = toEpochMilli(trade.getClose().getDate());
    return (openTime + closeTime) / 2.0;
  }

  @Override
  public Number getY(int series, int item) {
    return trades.get(item).getPnl();
  }

  public long getStartX(int item) {
    return toEpochMilli(trades.get(item).getOpen().getDate());
  }

  public long getEndX(int item) {
    return toEpochMilli(trades.get(item).getClose().getDate());
  }
}