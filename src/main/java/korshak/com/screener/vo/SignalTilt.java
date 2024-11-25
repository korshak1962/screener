package korshak.com.screener.vo;

import java.time.LocalDateTime;

public class SignalTilt extends Signal{
  public SignalTilt(LocalDateTime date, double price, SignalType action) {
    super(date, price, action);
  }
  double shortTilt = 0.0;
  double longTilt = 0.0;

  public double getShortTilt() {
    return shortTilt;
  }

  public void setShortTilt(Double shortTilt) {
    this.shortTilt = shortTilt;
  }

  public double getLongTilt() {
    return longTilt;
  }

  public void setLongTilt(double longTilt) {
    this.longTilt = longTilt;
  }

  @Override
  public String toString() {
    return "SignalTilt{" +
        "date=" + this.date +
        ", shortTilt=" + shortTilt +
        ", longTilt=" + longTilt +
        ", price=" + price +
        ", action=" + action +
        '}';
  }
}
