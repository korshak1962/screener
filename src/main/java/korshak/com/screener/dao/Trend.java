package korshak.com.screener.dao;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "trends")
public class Trend {
  @EmbeddedId
  private TrendKey id;

  private double extremum;
  private int trend;

  public Trend() {
  }

  public Trend(TrendKey id, double extremum, int trend) {
    this.id = id;
    this.extremum = extremum;
    this.trend = trend;
  }

  public TrendKey getId() {
    return id;
  }

  public void setId(TrendKey id) {
    this.id = id;
  }

  public double getExtremum() {
    return extremum;
  }

  public void setExtremum(double extremum) {
    this.extremum = extremum;
  }

  public int getTrend() {
    return trend;
  }

  public void setTrend(int trend) {
    this.trend = trend;
  }
}
