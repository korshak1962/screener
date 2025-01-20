package korshak.com.screener.dao;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
public class TrendKey implements Serializable {
  private String ticker;
  private LocalDateTime date;

  @Enumerated(EnumType.STRING)
  private TimeFrame timeframe;


  public TrendKey() {
  }

  public TrendKey(String ticker, LocalDateTime date, TimeFrame timeframe) {
    this.ticker = ticker;
    this.date = date;
    this.timeframe = timeframe;
  }

  // Getters and setters
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

  public TimeFrame getTimeframe() {
    return timeframe;
  }

  public void setTimeframe(TimeFrame timeframe) {
    this.timeframe = timeframe;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrendKey trendKey = (TrendKey) o;
    return Objects.equals(ticker, trendKey.ticker) &&
        Objects.equals(date, trendKey.date) &&
        timeframe == trendKey.timeframe;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ticker, date, timeframe);
  }
}