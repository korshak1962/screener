package korshak.com.screener.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.Trade;

public interface ChartService {
  void drawChart(List<? extends BasePrice> prices, List<? extends Signal> signals);

  void drawChart(List<? extends BasePrice> prices, List<? extends Signal> signals,
                 Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators,
                 List<Trade> tradesLong,
                 Map<String, NavigableMap<LocalDateTime, Double>> indicators);
}
