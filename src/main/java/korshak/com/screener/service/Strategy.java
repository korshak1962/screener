package korshak.com.screener.service;

import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.vo.Trade;

public interface Strategy {
  List<Trade> getTrades(List<? extends BasePrice> prices);
  String getName();
}
