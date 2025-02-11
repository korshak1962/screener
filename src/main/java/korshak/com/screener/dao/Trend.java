package korshak.com.screener.dao;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "trends")
public class Trend {
  @EmbeddedId
  private TrendKey id;

  private Double maxExtremum;
  private Double minExtremum;
  private int trend;

  public Trend() {
  }

  public Trend(TrendKey id, Double maxExtremum, Double minExtremum, int trend) {
    this.id = id;
    this.maxExtremum = maxExtremum;
    this.minExtremum = minExtremum;
    this.trend = trend;
  }

  public TrendKey getId() {
    return id;
  }

  public void setId(TrendKey id) {
    this.id = id;
  }

  public Double getMaxExtremum() {
    return maxExtremum;
  }

  public void setMaxExtremum(Double maxExtremum) {
    this.maxExtremum = maxExtremum;
  }

  public Double getMinExtremum() {
    return minExtremum;
  }

  public void setMinExtremum(Double minExtremum) {
    this.minExtremum = minExtremum;
  }

  public int getTrend() {
    return trend;
  }

  public void setTrend(int trend) {
    this.trend = trend;
  }
}