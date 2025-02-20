package korshak.com.screener.dao;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseRsi implements TiltableIndicator {
  @EmbeddedId
  private RsiKey id;
  private double value;
  private double tilt;

  public RsiKey getId() {
    return id;
  }

  public void setId(RsiKey id) {
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
}