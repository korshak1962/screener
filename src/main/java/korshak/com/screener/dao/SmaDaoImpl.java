package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SmaDaoImpl implements SmaDao {
  private final SmaHourRepository hourRepository;
  private final SmaDayRepository dayRepository;
  private final SmaWeekRepository weekRepository;
  private final SmaMonthRepository monthRepository;

  @Autowired
  public SmaDaoImpl(
      SmaHourRepository hourRepository,
      SmaDayRepository dayRepository,
      SmaWeekRepository weekRepository,
      SmaMonthRepository monthRepository) {
    this.hourRepository = hourRepository;
    this.dayRepository = dayRepository;
    this.weekRepository = weekRepository;
    this.monthRepository = monthRepository;
  }

  @Override
  public void deleteByTickerAndLength(String ticker, int length, TimeFrame timeFrame) {
    SmaRepository smaRepository = getSmaRepository(timeFrame);
    smaRepository.deleteByIdTickerAndIdLength(ticker, length);
  }

  private SmaRepository getSmaRepository(TimeFrame timeFrame) {
    SmaRepository smaRepository = null;
    switch (timeFrame) {
      case HOUR -> smaRepository = hourRepository;
      case DAY -> smaRepository = dayRepository;
      case WEEK -> smaRepository = weekRepository;
      case MONTH -> smaRepository = monthRepository;
    }
    return smaRepository;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void saveAll(List<? extends BaseSma> smaList, TimeFrame timeFrame) {
    switch (timeFrame) {
      case HOUR -> hourRepository.saveAll((List<SmaHour>) smaList);
      case DAY -> dayRepository.saveAll((List<SmaDay>) smaList);
      case WEEK -> weekRepository.saveAll((List<SmaWeek>) smaList);
      case MONTH -> monthRepository.saveAll((List<SmaMonth>) smaList);
    }
  }

  @Override
  public List<? extends BaseSma> findAllByTicker(String ticker, TimeFrame timeFrame, int length) {
    SmaRepository smaRepository = getSmaRepository(timeFrame);
    return smaRepository.findByIdTickerAndIdLengthOrderByIdDateAsc(ticker, length);
  }

  @Override
  public List<? extends BaseSma> findByDateRange(
      String ticker,
      LocalDateTime startDate,
      LocalDateTime endDate,
      TimeFrame timeFrame,
      int length) {
    SmaRepository smaRepository = getSmaRepository(timeFrame);
   return smaRepository.findByIdTickerAndIdLengthAndIdDateBetweenOrderByIdDateAsc(
        ticker, length, startDate, endDate);
  }

}