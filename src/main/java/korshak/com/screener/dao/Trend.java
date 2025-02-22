package korshak.com.screener.dao;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.text.DecimalFormat;
import java.util.Optional;

@Entity
@Table(name = "trends")
public class Trend {
  @EmbeddedId
  private TrendKey id;

  @Column(name = "max_extremum", nullable = false)
  private double maxExtremum;  // Changed from Double to double

  @Column(name = "min_extremum", nullable = false)
  private double minExtremum;  // Changed from Double to double

  private int trend;

  public Trend() {
  }

  public Trend(TrendKey id, double maxExtremum, double minExtremum, int trend) {
    this.id = id;
    this.maxExtremum = maxExtremum;
    this.minExtremum = minExtremum;
    this.trend = trend;
  }

  // Update getters and setters to use primitive double
  public double getMaxExtremum() {
    return maxExtremum;
  }

  public void setMaxExtremum(double maxExtremum) {
    this.maxExtremum = maxExtremum;
  }

  public double getMinExtremum() {
    return minExtremum;
  }

  public void setMinExtremum(double minExtremum) {
    this.minExtremum = minExtremum;
  }

  // Other getters and setters remain the same
  public TrendKey getId() {
    return id;
  }

  public void setId(TrendKey id) {
    this.id = id;
  }

  public int getTrend() {
    return trend;
  }

  public void setTrend(int trend) {
    this.trend = trend;
  }

  @Override
  public String toString() {
    DecimalFormat df = new DecimalFormat("#.##");
    return
        id.getDate().getDayOfMonth() +
        ", " + df.format(maxExtremum) +
        ", " + df.format(minExtremum) +
        ", " + trend;
  }
}