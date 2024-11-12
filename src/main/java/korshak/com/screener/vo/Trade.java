package korshak.com.screener.vo;

public class Trade {
  private final Signal open;
  private final Signal close;
  private Double pnl = null;

  public Trade(Signal open, Signal close) {
    this.open = open;
    this.close = close;
    pnl = (close.price  - open.price)*open.getAction();
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
}
