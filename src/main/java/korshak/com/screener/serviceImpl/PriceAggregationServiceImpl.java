package korshak.com.screener.serviceImpl;

import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDay;
import korshak.com.screener.dao.PriceDayRepository;
import korshak.com.screener.dao.PriceHour;
import korshak.com.screener.dao.PriceHourRepository;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceMin5;
import korshak.com.screener.dao.PriceMin5Repository;
import korshak.com.screener.dao.PriceMonth;
import korshak.com.screener.dao.PriceMonthRepository;
import korshak.com.screener.dao.PriceWeek;
import korshak.com.screener.dao.PriceWeekRepository;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.PriceAggregationService;
import org.springframework.stereotype.Service;

@Service
public class PriceAggregationServiceImpl implements PriceAggregationService {

  private final PriceMin5Repository priceMin5Repository;
  private final PriceHourRepository priceHourRepository;
  private final PriceDayRepository priceDayRepository;
  private final PriceWeekRepository priceWeekRepository;
  private final PriceMonthRepository priceMonthRepository;

  public PriceAggregationServiceImpl(
      PriceMin5Repository priceMin5Repository,
      PriceHourRepository priceHourRepository,
      PriceDayRepository priceDayRepository,
      PriceWeekRepository priceWeekRepository,
      PriceMonthRepository priceMonthRepository) {
    this.priceMin5Repository = priceMin5Repository;
    this.priceHourRepository = priceHourRepository;
    this.priceDayRepository = priceDayRepository;
    this.priceWeekRepository = priceWeekRepository;
    this.priceMonthRepository = priceMonthRepository;
  }

  @Transactional
  public void aggregateData(String ticker, TimeFrame timeFrame) {
    List<PriceMin5> min5Data = priceMin5Repository.findById_Ticker(ticker);
System.out.println("found " + min5Data.size());
    // Group by the specified timeframe
    Map<LocalDateTime, List<PriceMin5>> groupedPrices = min5Data.stream()
        .collect(Collectors.groupingBy(price -> truncateToTimeFrame(
            price.getId().getDate(),
            timeFrame)));

    // Create and save aggregated prices
    switch (timeFrame) {
      case HOUR:
        List<PriceHour> hourPrices = groupedPrices.entrySet().stream()
            .map(entry -> createAggregatedPrice(entry.getKey(), entry.getValue(), ticker, new PriceHour()))
            .collect(Collectors.toList());
        System.out.println("hourPrices to be saved = " + hourPrices.size());
        priceHourRepository.saveAll(hourPrices);
        break;
      case DAY:
        List<PriceDay> dayPrices = groupedPrices.entrySet().stream()
            .map(entry -> createAggregatedPrice(entry.getKey(), entry.getValue(), ticker, new PriceDay()))
            .collect(Collectors.toList());
        System.out.println("dayPrices to be saved = " + dayPrices.size());
        priceDayRepository.saveAll(dayPrices);
        break;
      case WEEK:
        List<PriceWeek> weekPrices = groupedPrices.entrySet().stream()
            .map(entry -> createAggregatedPrice(entry.getKey(), entry.getValue(), ticker, new PriceWeek()))
            .collect(Collectors.toList());
        System.out.println("weekPrices to be saved = " + weekPrices.size());
        priceWeekRepository.saveAll(weekPrices);
        break;
      case MONTH:
        List<PriceMonth> monthPrices = groupedPrices.entrySet().stream()
            .map(entry -> createAggregatedPrice(entry.getKey(), entry.getValue(), ticker, new PriceMonth()))
            .collect(Collectors.toList());
        System.out.println("monthPrices to be saved = " + monthPrices.size());
        priceMonthRepository.saveAll(monthPrices);
        break;
    }
  }

  private LocalDateTime truncateToTimeFrame(LocalDateTime dateTime, TimeFrame timeFrame) {
    switch (timeFrame) {
      case HOUR:
        return dateTime.truncatedTo(ChronoUnit.HOURS);
      case DAY:
        return dateTime.truncatedTo(ChronoUnit.DAYS);
      case WEEK:
        return dateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .truncatedTo(ChronoUnit.DAYS);
      case MONTH:
        return dateTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
      default:
        throw new IllegalArgumentException("Unsupported time frame: " + timeFrame);
    }
  }

  private <T extends BasePrice> T createAggregatedPrice(
      LocalDateTime dateTime,
      List<PriceMin5> prices,
      String ticker,
      T aggregatedPrice) {

    PriceKey key = new PriceKey();
    key.setTicker(ticker);
    key.setDate(dateTime);
    aggregatedPrice.setId(key);

    // Sort prices by time to ensure correct open/close
    prices.sort(Comparator.comparing(p -> p.getId().getDate()));

    aggregatedPrice.setOpen(prices.get(0).getOpen());
    aggregatedPrice.setClose(prices.get(prices.size() - 1).getClose());

    double high = prices.stream()
        .mapToDouble(PriceMin5::getHigh)
        .max()
        .orElse(0.0);

    double low = prices.stream()
        .mapToDouble(PriceMin5::getLow)
        .min()
        .orElse(0.0);

    long totalVolume = prices.stream()
        .mapToLong(PriceMin5::getVolume)
        .sum();

    aggregatedPrice.setHigh(high);
    aggregatedPrice.setLow(low);
    aggregatedPrice.setVolume(totalVolume);

    return aggregatedPrice;
  }
}
