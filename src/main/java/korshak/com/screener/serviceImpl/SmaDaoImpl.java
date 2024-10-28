package korshak.com.screener.serviceImpl;

import java.util.List;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.SmaDay;
import korshak.com.screener.dao.SmaDayRepository;
import korshak.com.screener.dao.SmaHour;
import korshak.com.screener.dao.SmaHourRepository;
import korshak.com.screener.dao.SmaMonth;
import korshak.com.screener.dao.SmaMonthRepository;
import korshak.com.screener.dao.SmaWeek;
import korshak.com.screener.dao.SmaWeekRepository;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.SmaDao;
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
    switch (timeFrame) {
      case HOUR -> hourRepository.deleteByIdTickerAndIdLength(ticker, length);
      case DAY -> dayRepository.deleteByIdTickerAndIdLength(ticker, length);
      case WEEK -> weekRepository.deleteByIdTickerAndIdLength(ticker, length);
      case MONTH -> monthRepository.deleteByIdTickerAndIdLength(ticker, length);
    }
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
}