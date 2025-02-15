package korshak.com.screener.dao;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
public class RsiKey implements Serializable {
  private String ticker;
  private LocalDateTime date;
  private int length;  // RSI length/period

  public RsiKey() {
  }

  public RsiKey(String ticker, LocalDateTime date, int length) {
    this.ticker = ticker;
    this.date = date;
    this.length = length;
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }

  public LocalDateTime getDate() {
    return date;
  }

  public void setDate(LocalDateTime date) {
    this.date = date;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RsiKey rsiKey = (RsiKey) o;
    return length == rsiKey.length &&
        Objects.equals(ticker, rsiKey.ticker) &&
        Objects.equals(date, rsiKey.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ticker, date, length);
  }
}