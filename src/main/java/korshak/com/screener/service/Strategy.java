package korshak.com.screener.service;

import java.time.LocalDateTime;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.vo.Signal;

public interface Strategy {
  List<? extends Signal> getSignalsLong();
  List<? extends Signal> getSignalsShort();
void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate, LocalDateTime endDate);
  String getName();
  List<? extends BasePrice> getPrices();
}
