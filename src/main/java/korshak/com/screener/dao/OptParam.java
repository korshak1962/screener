package korshak.com.screener.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "opt_params")
@IdClass(OptParamKey.class)
public class OptParam implements Serializable {

  @Id
  private String ticker;

  @Id
  private String param;

  @Id
  @Enumerated(EnumType.STRING)
  private TimeFrame timeframe;

  private double value; // Not null

  // Default constructor
  public OptParam() {
  }

  // Constructor with all fields
  public OptParam(String ticker, String param, TimeFrame timeframe, double value) {
    this.ticker = ticker;
    this.param = param;
    this.timeframe = timeframe;
    this.value = value;
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

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OptParam optParam = (OptParam) o;
    return Objects.equals(ticker, optParam.ticker) &&
        Objects.equals(param, optParam.param) &&
        timeframe == optParam.timeframe;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ticker, param, timeframe);
  }

  @Override
  public String toString() {
    return "OptParam{" +
        "ticker='" + ticker + '\'' +
        ", param='" + param + '\'' +
        ", timeframe=" + timeframe +
        ", value=" + value +
        '}';
  }
}