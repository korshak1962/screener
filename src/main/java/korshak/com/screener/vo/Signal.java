package korshak.com.screener.vo;

import java.time.LocalDateTime;

public class Signal {

    private  LocalDateTime date;
    double price;
    SignalType action;  // -1 sell ;1 buy


  public Signal(LocalDateTime date, double price, SignalType action) {
    this.date = date;
    this.price = price;
    this.action = action;
  }
  public LocalDateTime getDate() {
    return date;
  }

  public void setDate(LocalDateTime date) {
    this.date = date;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public SignalType getAction() {
    return action;
  }

  public void setAction(SignalType action) {
    this.action = action;
  }
}
