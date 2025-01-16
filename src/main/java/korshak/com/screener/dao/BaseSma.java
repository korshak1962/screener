package korshak.com.screener.dao;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class BaseSma {
  @EmbeddedId
  private SmaKey id;
  private double value;
  private double tilt;
  private double yield;

  public SmaKey getId() {
    return id;
  }

  public void setId(SmaKey id) {
    this.id = id;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public double getTilt() {
    return tilt;
  }

  public void setTilt(double tilt) {
    this.tilt = tilt;
  }

  public double getYield() {
    return yield;
  }

  public void setYield(double yield) {
    this.yield = yield;
  }
}
