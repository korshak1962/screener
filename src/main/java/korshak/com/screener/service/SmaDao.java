package korshak.com.screener.service;

import java.util.List;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.TimeFrame;

public interface SmaDao {
  void deleteByTickerAndLength(String ticker, int length, TimeFrame timeFrame);
  void saveAll(List<? extends BaseSma> smaList, TimeFrame timeFrame);
}
