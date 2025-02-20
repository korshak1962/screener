package korshak.com.screener.dao;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

// Better approach using @EmbeddedId:
@Embeddable
public class PriceKey implements Serializable {
  private String ticker;
  private LocalDateTime date; // or LocalDate for daily prices

  // Constructors
  public PriceKey() {
  }

  public PriceKey(String ticker, LocalDateTime date) {
    this.ticker = ticker;
    this.date = date;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PriceKey priceKey = (PriceKey) o;
    return Objects.equals(ticker, priceKey.ticker) &&
        Objects.equals(date, priceKey.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ticker, date);
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }

  public LocalDateTime getDate() {
    return date;
  }

  public void setDate(LocalDateTime date) {
    this.date = date;
  }
}
