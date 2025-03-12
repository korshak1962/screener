package korshak.com.screener.dao;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "opt_params")
public class OptParam {
  @EmbeddedId
  private OptParamKey id;

  @Enumerated(EnumType.STRING)
  private TimeFrame timeframe;

  private Double value;

  @Column(name = "value_string")
  private String valueString;

  public OptParam() {
  }

  public OptParam(String ticker, String paramName, String strategy, TimeFrame timeframe, Double value, String valueString) {
    this.id = new OptParamKey(ticker, paramName, strategy);
    this.timeframe = timeframe;
    this.value = value;
    this.valueString = valueString;
  }

  public OptParamKey getId() {
    return id;
  }

  public void setId(OptParamKey id) {
    this.id = id;
  }

  public TimeFrame getTimeframe() {
    return timeframe;
  }

  public void setTimeframe(TimeFrame timeframe) {
    this.timeframe = timeframe;
  }

  public Double getValue() {
    return value;
  }

  public void setValue(Double value) {
    this.value = value;
  }

  public String getValueString() {
    return valueString;
  }

  public void setValueString(String valueString) {
    this.valueString = valueString;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OptParam optParam = (OptParam) o;
    return Objects.equals(id, optParam.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "OptParam{" +
        "id=" + id +
        ", timeframe=" + timeframe +
        ", value=" + value +
        ", valueString='" + valueString + '\'' +
        '}';
  }

  @Embeddable
  public static class OptParamKey implements Serializable {
    private String ticker;
    private String param;
    private String strategy;

    public OptParamKey() {
    }

    public OptParamKey(String ticker, String paramName, String strategy) {
      this.ticker = ticker;
      this.param = paramName;
      this.strategy = strategy;
    }

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

    public String getStrategy() {
      return strategy;
    }

    public void setStrategy(String strategy) {
      this.strategy = strategy;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      OptParamKey that = (OptParamKey) o;
      return Objects.equals(ticker, that.ticker) &&
          Objects.equals(param, that.param) &&
          Objects.equals(strategy, that.strategy);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ticker, param, strategy);
    }

    @Override
    public String toString() {
      return "OptParamKey{" +
          "ticker='" + ticker + '\'' +
          ", paramName='" + param + '\'' +
          ", strategy='" + strategy + '\'' +
          '}';
    }
  }
}