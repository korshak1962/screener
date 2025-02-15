package korshak.com.screener.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "rsi_hour")
public class RsiHour extends BaseRsi{}
