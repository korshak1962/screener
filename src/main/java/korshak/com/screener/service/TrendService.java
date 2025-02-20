package korshak.com.screener.service;

import java.util.List;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.Trend;

public interface TrendService {
  List<Trend> calculateAndStorePriceTrend(String ticker, TimeFrame timeFrame);

  void calculateAndStorePriceTrendForAllTimeframes(String ticker);
}
