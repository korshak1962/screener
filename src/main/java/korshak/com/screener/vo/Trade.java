package korshak.com.screener.vo;

public class Trade {
  private final Signal open;
  private final Signal close;
  private Double pnl = null;
  private double maxPainPercent;

  public Trade(Signal open, Signal close) {
    this.open = open;
    this.close = close;
    pnl = (close.price  - open.price);
    if (close.getSignalType()==SignalType.ShortClose){ // short trade
      pnl=-pnl;
    }
  }

  public Double getPnl() {
    return pnl;
  }

  public Signal getOpen() {
    return open;
  }

  public Signal getClose() {
    return close;
  }

  public double getMaxPainPercent() {
    return maxPainPercent;
  }

 public void setMaxPainPercent(double maxPainPercent) {
    this.maxPainPercent = maxPainPercent;
  }
}
