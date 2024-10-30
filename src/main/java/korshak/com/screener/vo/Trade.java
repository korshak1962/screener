package korshak.com.screener.vo;

import java.time.LocalDateTime;

public class Trade {

  private  LocalDateTime date;
  double price;
  int action;  // -1 sell ;1 buy
  int value;


  public Trade(LocalDateTime date, double price, int action, int value) {
    this.date = date;
    this.price = price;
    this.action = action;
    this.value = value;
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

  public int getAction() {
    return action;
  }

  public void setAction(int action) {
    this.action = action;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }
}
