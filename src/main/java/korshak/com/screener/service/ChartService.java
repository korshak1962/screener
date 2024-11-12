package korshak.com.screener.service;

import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.Trade;

public interface ChartService {
  void drawChart(List<? extends BasePrice> prices, List<Signal> signals);
  void drawChart(List<? extends BasePrice> prices, List<Signal> signals,
                 List<? extends BaseSma> smaList, List<Trade> tradesLong);
}
