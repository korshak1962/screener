package korshak.com.screener.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.Trade;

public interface ChartService {
  void drawChart(List<? extends BasePrice> prices, List<Signal> signals);
  void drawChart(List<? extends BasePrice> prices, List<Signal> signals,
                 List<? extends BaseSma> smaList, List<Trade> tradesLong,
                 Map<String, TreeMap<LocalDateTime, Double>> indicators);
}
