package korshak.com.screener.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import korshak.com.screener.dao.*;
import korshak.com.screener.vo.MinMax;

public interface TrendService {
  List<Trend> calculateAndStorePriceTrend(String ticker, TimeFrame timeFrame);
  void calculateAndStorePriceTrendForAllTimeframes(String ticker);
}
