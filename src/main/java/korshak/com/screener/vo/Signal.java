package korshak.com.screener.vo;

import java.time.LocalDateTime;

public class Signal {

  final LocalDateTime date;
  final double price;
  final SignalType signalType;  // -1 sell ;1 buy
  final String comment;


  public Signal(LocalDateTime date, double price, SignalType action, String comment) {
    this.date = date;
    this.price = price;
    this.signalType = action;
    this.comment = comment;
  }

  public LocalDateTime getDate() {
    return date;
  }

  public double getPrice() {
    return price;
  }

  public SignalType getSignalType() {
    return signalType;
  }

  public String getComment() {
    return comment;
  }

  @Override
  public String toString() {
    return "Signal{" +
        "date=" + date +
        ", price=" + price +
        ", signalType=" + signalType +
        ", comment='" + comment + '\'' +
        '}';
  }
}
