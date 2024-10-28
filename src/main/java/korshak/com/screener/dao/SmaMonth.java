package korshak.com.screener.dao;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_month_sma")
public class SmaMonth extends BaseSma {}