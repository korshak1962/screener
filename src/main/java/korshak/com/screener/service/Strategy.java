package korshak.com.screener.service;

import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.vo.Signal;

public interface Strategy {
  List<Signal> getTrades(List<? extends BasePrice> prices);
  String getName();
}
