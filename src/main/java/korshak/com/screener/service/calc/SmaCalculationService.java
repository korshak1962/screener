package korshak.com.screener.service.calc;

import java.time.LocalDateTime;
import java.util.List;
import korshak.com.screener.dao.BaseSma;
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

  void calculateSMAForAllTimeFrame(String ticker, int length);

  void calculateSMAForAllTimeFrameAndTickers(int length);

  List<? extends BaseSma> calculateIncrementalSMA(String ticker, int length, TimeFrame timeFrame);

  void calculateIncrementalSMAForAllTickersAndTimeFrames(int length);

  void calculateIncrementalSMAForAllTimeFrames(String ticker, int length);
}
