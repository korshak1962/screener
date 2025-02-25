package korshak.com.screener.vo;

import java.time.LocalDateTime;

public class SignalTilt extends Signal {
  private final double tilt;
  private final double trendTilt;

  public SignalTilt(LocalDateTime date, double price, SignalType action, double tilt,
                    double trendTilt) {
    super(date, price, action);
    this.tilt = tilt;
    this.trendTilt = trendTilt;
  }

  public double getTilt() {
    return tilt;
  }

  public double getTrendTilt() {
    return trendTilt;
  }

  @Override
  public String toString() {
    return "SignalTilt{" +
        "date=" + this.date +
        ", shortTilt=" + tilt +
        ", longTilt=" + trendTilt +
        ", price=" + price +
        ", action=" + signalType +
        '}';
  }
}
