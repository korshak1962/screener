package korshak.com.screener.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_day_sma")
public class SmaDay extends BaseSma {
}