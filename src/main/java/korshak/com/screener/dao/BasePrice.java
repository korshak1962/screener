package korshak.com.screener.dao;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BasePrice {
  @EmbeddedId
  private PriceKey id;
  private double open;
  private double high;
  private double low;
  private double close;
  private long volume;

  public BasePrice() {
  }

  public BasePrice(PriceKey id, double open, double high, double low, double close, long volume) {
    this.id = id;
    this.open = open;
    this.high = high;
    this.low = low;
    this.close = close;
    this.volume = volume;
  }

  public PriceKey getId() {
    return id;
  }

  public void setId(PriceKey id) {
    this.id = id;
  }

  public double getOpen() {
    return open;
  }

  public void setOpen(double open) {
    this.open = open;
  }

  public double getHigh() {
    return high;
  }

  public void setHigh(double high) {
    this.high = high;
  }

  public double getLow() {
    return low;
  }

  public void setLow(double low) {
    this.low = low;
  }

  public double getClose() {
    return close;
  }

  public void setClose(double close) {
    this.close = close;
  }

  public long getVolume() {
    return volume;
  }

  public void setVolume(long volume) {
    this.volume = volume;
  }
}
