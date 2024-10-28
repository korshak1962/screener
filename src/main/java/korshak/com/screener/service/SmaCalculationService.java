package korshak.com.screener.service;

import java.time.LocalDateTime;
import korshak.com.screener.dao.TimeFrame;

public interface SmaCalculationService {

   void calculateSMA(
      String ticker,
      int length,
      LocalDateTime startDate,
      LocalDateTime endDate,
      TimeFrame timeFrame);

void calculateSMA(
    String ticker,
    int length,
    TimeFrame timeFrame);
}
