package korshak.com.screener.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_min5")
public class PriceMin5 extends BasePrice {
  public PriceMin5() {
  }

  public PriceMin5(PriceKey id, double open, double high, double low, double close, long volume) {
    super(id, open, high, low, close, volume);
  }
}

