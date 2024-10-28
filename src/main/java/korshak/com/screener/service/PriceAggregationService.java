package korshak.com.screener.service;

import korshak.com.screener.dao.TimeFrame;

public interface PriceAggregationService {
   void aggregateData(String ticker, TimeFrame timeFrame);
}
