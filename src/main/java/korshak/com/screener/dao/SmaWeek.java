package korshak.com.screener.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_week_sma")
public class SmaWeek extends BaseSma {}
