package korshak.com.screener.dao;

import java.io.Serializable;
import java.util.Objects;

public class OptParamKey implements Serializable {
  private String ticker;
  private String param;
  private TimeFrame timeframe;

  // Default constructor
  public OptParamKey() {
  }

  // Constructor with all fields
  public OptParamKey(String ticker, String param, TimeFrame timeframe) {
    this.ticker = ticker;
    this.param = param;
    this.timeframe = timeframe;
  }

  // Getters and setters
  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }

  public String getParam() {
    return param;
  }

  public void setParam(String param) {
    this.param = param;
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
    OptParamKey that = (OptParamKey) o;
    return Objects.equals(ticker, that.ticker) &&
        Objects.equals(param, that.param) &&
        timeframe == that.timeframe;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ticker, param, timeframe);
  }
}