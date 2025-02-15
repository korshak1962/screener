package korshak.com.screener.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "rsi_day")
public class RsiDay extends BaseRsi {}
