package korshak.com.screener.service.calc;

import java.time.LocalDateTime;
import java.util.List;
import korshak.com.screener.dao.BaseRsi;
import korshak.com.screener.dao.TimeFrame;

public interface RsiService {
  void calculateRsi(String ticker, int length, TimeFrame timeFrame);

  void calculateRsi(String ticker, int length, LocalDateTime startDate, LocalDateTime endDate,
                    TimeFrame timeFrame);

  void calculateRsiForAllTimeFrames(String ticker, int length);

  void calculateRsiForAllTimeFramesAndTickers(int length);

  List<? extends BaseRsi> calculateIncrementalRsi(String ticker, int length, TimeFrame timeFrame);

  void calculateIncrementalRsiForAllTickersAndTimeFrames(int length);

  void calculateIncrementalRsiForAllTimeFrames(String ticker, int length);
}
