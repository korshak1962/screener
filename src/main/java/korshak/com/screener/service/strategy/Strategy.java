package korshak.com.screener.service.strategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.vo.Signal;

public interface Strategy {
  List<? extends Signal> getSignalsLong();
  List<? extends Signal> getSignalsShort();
void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate, LocalDateTime endDate);
  String getName();
  List<? extends BasePrice> getPrices();
  Map<String, NavigableMap<LocalDateTime, Double>> getIndicators();
  Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators();
  TimeFrame getTimeFrame();
  String getTicker();
  LocalDateTime getStartDate();
  LocalDateTime getEndDate();



}
