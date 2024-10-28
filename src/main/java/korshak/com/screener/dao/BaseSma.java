package korshak.com.screener.dao;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseSma {
  @EmbeddedId
  private SmaKey id;
  private double value;

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
}
