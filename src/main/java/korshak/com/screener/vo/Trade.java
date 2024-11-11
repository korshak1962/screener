package korshak.com.screener.vo;

public class Trade {
  Signal open;
  Signal close;

  public Trade(Signal open, Signal close) {
    this.open = open;
    this.close = close;
  }

  public double getPnl() {
    return close.price*close.action -open.price*open.action;
  }
}
