package korshak.com.screener.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "opt_params")
public class OptParam {
  @EmbeddedId
  private OptParamKey id;

  @Enumerated(EnumType.STRING)
  private TimeFrame timeframe;

  @Column(name = "strategy_class")
  private String strategyClass;

  private double value;

  @Column(name = "value_string")
  private String valueString;

  @Column(name = "min", nullable = false, columnDefinition = "FLOAT DEFAULT 0")
  private float min = 0.0f;

  @Column(name = "max", nullable = false, columnDefinition = "FLOAT DEFAULT 0")
  private float max = 0.0f;

  @Column(name = "step", nullable = false, columnDefinition = "FLOAT DEFAULT 0")
  private float step = 0.0f;

  // Constructors
  public OptParam() {
  }


    public OptParam(String ticker, String paramName, String strategy, String caseId,
                  TimeFrame timeframe, String strategyClass, double value, String valueString,
                  float min, float max, float step) {
    this.id = new OptParamKey(ticker, paramName, strategy, caseId);
    this.timeframe = timeframe;
    this.strategyClass = strategyClass;
    this.value = value;
    this.valueString = valueString;
    this.min = min;
    this.max = max;
    this.step = step;
  }

  // Getters and setters
  public OptParamKey getId() {
    return id;
  }

  public void setId(OptParamKey id) {
    this.id = id;
  }

  public String getCaseId() {
    return id != null ? id.getCaseId() : null;
  }

  public TimeFrame getTimeframe() {
    return timeframe;
  }

  public void setTimeframe(TimeFrame timeframe) {
    this.timeframe = timeframe;
  }

  public String getStrategyClass() {
    return strategyClass;
  }

  public void setStrategyClass(String strategyClass) {
    this.strategyClass = strategyClass;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public String getValueString() {
    return valueString;
  }

  public void setValueString(String valueString) {
    this.valueString = valueString;
  }

  public float getMin() {
    return min;
  }

  public void setMin(float min) {
    this.min = min;
  }

  public float getMax() {
    return max;
  }

  public void setMax(float max) {
    this.max = max;
  }

  public float getStep() {
    return step;
  }

  public void setStep(float step) {
    this.step = step;
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
    return Objects.equals(id, optParam.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return
        "param=" + id.param +
        ", value=" + value +
        ", valueString='" + valueString;
  }

  @Embeddable
  public static class OptParamKey implements Serializable {
    private String ticker;
    private String param;
    private String strategy;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    public OptParamKey() {
      this.caseId = "Single"; // Default value
    }

    public OptParamKey(String ticker, String paramName, String strategy, String caseId) {
      this.ticker = ticker;
      this.param = paramName;
      this.strategy = strategy;
      this.caseId = caseId != null ? caseId : "Single";
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

    public String getCaseId() {
      return caseId;
    }

    public void setCaseId(String caseId) {
      this.caseId = caseId != null ? caseId : "Single";
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
          Objects.equals(strategy, that.strategy) &&
          Objects.equals(caseId, that.caseId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ticker, param, strategy, caseId);
    }

    @Override
    public String toString() {
      return "OptParamKey{" +
          "ticker='" + ticker + '\'' +
          ", paramName='" + param + '\'' +
          ", strategy='" + strategy + '\'' +
          ", caseId='" + caseId + '\'' +
          '}';
    }
  }
}