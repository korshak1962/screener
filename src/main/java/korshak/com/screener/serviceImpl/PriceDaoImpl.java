package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDayRepository;
import korshak.com.screener.dao.PriceHourRepository;
import korshak.com.screener.dao.PriceMonthRepository;
import korshak.com.screener.dao.PriceWeekRepository;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.PriceDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PriceDaoImpl implements PriceDao {
  private final PriceHourRepository hourRepository;
  private final PriceDayRepository dayRepository;
  private final PriceWeekRepository weekRepository;
  private final PriceMonthRepository monthRepository;

  @Autowired
  public PriceDaoImpl(
      PriceHourRepository hourRepository,
      PriceDayRepository dayRepository,
      PriceWeekRepository weekRepository,
      PriceMonthRepository monthRepository) {
    this.hourRepository = hourRepository;
    this.dayRepository = dayRepository;
    this.weekRepository = weekRepository;
    this.monthRepository = monthRepository;
  }

  @Override
  public List<? extends BasePrice> findByDateRange(
      String ticker,
      LocalDateTime startDate,
      LocalDateTime endDate,
      TimeFrame timeFrame) {
    return switch (timeFrame) {
      case HOUR -> hourRepository.findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
          ticker, startDate, endDate);
      case DAY -> dayRepository.findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
          ticker, startDate, endDate);
      case WEEK -> weekRepository.findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
          ticker, startDate, endDate);
      case MONTH -> monthRepository.findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
          ticker, startDate, endDate);
    };
  }

  @Override
  public List<? extends BasePrice> findAllByTicker(String ticker, TimeFrame timeFrame) {
    return switch (timeFrame) {
      case HOUR -> hourRepository.findByIdTickerOrderByIdDateAsc(ticker);
      case DAY -> dayRepository.findByIdTickerOrderByIdDateAsc(ticker);
      case WEEK -> weekRepository.findByIdTickerOrderByIdDateAsc(ticker);
      case MONTH -> monthRepository.findByIdTickerOrderByIdDateAsc(ticker);
    };
  }
}
