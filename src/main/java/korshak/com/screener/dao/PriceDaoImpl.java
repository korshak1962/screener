package korshak.com.screener.dao;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PriceDaoImpl implements PriceDao {
  private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
  private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

  private final PriceMin5Repository min5Repository;
  private final PriceHourRepository hourRepository;
  private final PriceDayRepository dayRepository;
  private final PriceWeekRepository weekRepository;
  private final PriceMonthRepository monthRepository;

  @Autowired
  public PriceDaoImpl(
      PriceMin5Repository min5Repository,
      PriceHourRepository hourRepository,
      PriceDayRepository dayRepository,
      PriceWeekRepository weekRepository,
      PriceMonthRepository monthRepository) {
    this.min5Repository = min5Repository;
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
    // Default behavior - main session only
    return findByDateRange(ticker, startDate, endDate, timeFrame, false);
  }

  public List<? extends BasePrice> findByDateRange(
      String ticker,
      LocalDateTime startDate,
      LocalDateTime endDate,
      TimeFrame timeFrame,
      boolean includeExtendedHours) {

    List<? extends BasePrice> prices = switch (timeFrame) {
      case MIN5 -> min5Repository.findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
          ticker, startDate, endDate);
      case HOUR -> hourRepository.findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
          ticker, startDate, endDate);
      case DAY -> dayRepository.findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
          ticker, startDate, endDate);
      case WEEK -> weekRepository.findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
          ticker, startDate, endDate);
      case MONTH -> monthRepository.findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
          ticker, startDate, endDate);
    };

    // For hour timeframe, apply trading hours filter if needed
    if ((timeFrame == TimeFrame.HOUR || timeFrame == TimeFrame.MIN5) && !includeExtendedHours) {
      return filterMainSessionPrices(prices);
    }

    return prices;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends BasePrice> List<T> saveAll(List<T> prices) {
    if (prices == null || prices.isEmpty()) {
      return prices;
    }

    // Get the first element to determine the type
    T firstElement = prices.get(0);

    return switch (firstElement) {
      case PriceMin5 ignored -> (List<T>) min5Repository.saveAll((List<PriceMin5>) prices);
      case PriceHour ignored -> (List<T>) hourRepository.saveAll((List<PriceHour>) prices);
      case PriceDay ignored -> (List<T>) dayRepository.saveAll((List<PriceDay>) prices);
      case PriceWeek ignored -> (List<T>) weekRepository.saveAll((List<PriceWeek>) prices);
      case PriceMonth ignored -> (List<T>) monthRepository.saveAll((List<PriceMonth>) prices);
      case null -> throw new IllegalArgumentException("First element is null");
      default -> throw new IllegalArgumentException("Unsupported price type: " + firstElement.getClass());
    };
  }

  @Override
  public Set<String> findUniqueTickers() {
    return min5Repository.findUniqueTickers();
  }


  @Override
  public List<? extends BasePrice> findAllByTicker(String ticker, TimeFrame timeFrame) {
    // Default behavior - main session only
    return findAllByTicker(ticker, timeFrame, false);
  }

  public List<? extends BasePrice> findAllByTicker(String ticker, TimeFrame timeFrame, boolean includeExtendedHours) {
    List<? extends BasePrice> prices = switch (timeFrame) {
      case MIN5 -> min5Repository.findByIdTickerOrderByIdDateAsc(ticker);
      case HOUR -> hourRepository.findByIdTickerOrderByIdDateAsc(ticker);
      case DAY -> dayRepository.findByIdTickerOrderByIdDateAsc(ticker);
      case WEEK -> weekRepository.findByIdTickerOrderByIdDateAsc(ticker);
      case MONTH -> monthRepository.findByIdTickerOrderByIdDateAsc(ticker);
    };
    // For MIN5 timeframe apply trading hours filter if needed
    if (timeFrame == TimeFrame.MIN5 && !includeExtendedHours) {
      return filterMainSessionPrices(prices);
    }

    return prices;
  }

  private List<? extends BasePrice> filterMainSessionPrices(List<? extends BasePrice> prices) {
    return prices.stream()
        .filter(price -> {
          LocalTime time = price.getId().getDate().toLocalTime();
          return time.isAfter(MARKET_OPEN) && !time.isAfter(MARKET_CLOSE);
        })
        .collect(Collectors.toList());
  }
}