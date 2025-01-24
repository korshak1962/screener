package korshak.com.screener.vo;

import java.time.LocalDateTime;

public class Signal {

  final LocalDateTime date;
  final double price;
  final SignalType signalType;  // -1 sell ;1 buy
  String comment;


  public Signal(LocalDateTime date, double price, SignalType action) {
    this.date = date;
    this.price = price;
    this.signalType = action;
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

  public void setComment(String comment) {
    this.comment = comment;
  }
}
