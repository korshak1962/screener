package korshak.com.screener.service;

import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.vo.Trade;

public interface ChartService {
  void drawChart(List<? extends BasePrice> prices, List<Trade> trades);
}
