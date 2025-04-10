package korshak.com.screener.serviceImpl.calc;

import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceDay;
import korshak.com.screener.dao.PriceHour;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceMonth;
import korshak.com.screener.dao.PriceWeek;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.calc.PriceAggregationService;
import org.springframework.stereotype.Service;

@Service
public class PriceAggregationServiceImpl implements PriceAggregationService {

  private final PriceDao priceDao;

  public PriceAggregationServiceImpl(
      PriceDao priceDao) {
    this.priceDao = priceDao;
  }

  @Override
  @Transactional
  public void aggregateAllTickers() {
    Set<String> tickers = priceDao.findUniqueTickers();
    int totalTickers = tickers.size();
    int currentTicker = 0;

    System.out.println("Starting aggregation for " + totalTickers + " tickers");

    for (String ticker : tickers) {
      currentTicker++;
      System.out.println(
          "Processing ticker " + ticker + " (" + currentTicker + "/" + totalTickers + ")");

      try {
        aggregateAllTimeFrames(ticker);
      } catch (Exception e) {
        System.err.println("Error processing ticker " + ticker + ": " + e.getMessage());
        e.printStackTrace();
        // Continue with next ticker instead of breaking the entire process
      }
    }

    System.out.println("Completed aggregation for all tickers");
  }

  @Override
  @Transactional
  public void aggregateAllTimeFrames(String ticker) {
    System.out.println("Starting hierarchical aggregation for ticker: " + ticker);

    // Aggregate hierarchically
    try {
      System.out.println("Aggregating HOUR data");
      aggregateData(ticker, TimeFrame.HOUR);  // MIN5 -> HOUR

      System.out.println("Aggregating DAY data");
      aggregateData(ticker, TimeFrame.DAY);   // HOUR -> DAY

      System.out.println("Aggregating WEEK data");
      aggregateData(ticker, TimeFrame.WEEK);  // DAY -> WEEK

      System.out.println("Aggregating MONTH data");
      aggregateData(ticker, TimeFrame.MONTH); // WEEK -> MONTH

      System.out.println("Completed aggregation for ticker: " + ticker);
    } catch (Exception e) {
      System.err.println(
          "Error during hierarchical aggregation for ticker " + ticker + ": " + e.getMessage());
      throw e;
    }
  }

  @Override
  @Transactional
  public void aggregateData(String ticker, TimeFrame timeFrame) {
    // Get the source data based on the timeframe
    TimeFrame sourceTimeFrame = getSourceTimeFrame(timeFrame);
    List<? extends BasePrice> pricesOfLowTimeframe =
        priceDao.findAllByTicker(ticker, sourceTimeFrame);

    if (pricesOfLowTimeframe.isEmpty()) {
      System.out.println(
          "No source data found for " + ticker + " at " + sourceTimeFrame + " level");
      return;
    }

    // Get existing aggregated data to find gaps
    List<? extends BasePrice> existingData = priceDao.findAllByTicker(ticker, timeFrame);

    // Get date ranges that need aggregation
    LocalDateTime firstDate = pricesOfLowTimeframe.get(0).getId().getDate();
    LocalDateTime lastDate =
        pricesOfLowTimeframe.get(pricesOfLowTimeframe.size() - 1).getId().getDate();

    // Filter source data to only include periods not already aggregated
    List<? extends BasePrice> dataToAggregate = filterDataForMissingPeriods(
        pricesOfLowTimeframe, existingData, timeFrame);

    if (dataToAggregate.isEmpty()) {
      System.out.println("No new data to aggregate for " + ticker + " at " + timeFrame + " level");
      return;
    }

    // Group by the specified timeframe
    Map<LocalDateTime, List<BasePrice>> groupedPrices = dataToAggregate.stream()
        .map(price -> (BasePrice) price)  // Cast to BasePrice
        .collect(Collectors.groupingBy(
            price -> truncateToTimeFrame(price.getId().getDate(), timeFrame)
        ));

    // Create and save aggregated prices
    switch (timeFrame) {
      case HOUR -> {
        List<PriceHour> hourPrices = groupedPrices.entrySet().stream()
            .map(entry -> createAggregatedPrice(entry.getKey(), entry.getValue(), ticker,
                new PriceHour()))
            .collect(Collectors.toList());
        System.out.println("Saving " + hourPrices.size() + " new hour prices");
        priceDao.saveAll(hourPrices);
      }
      case DAY -> {
        List<PriceDay> dayPrices = groupedPrices.entrySet().stream()
            .map(entry -> createAggregatedPrice(entry.getKey(), entry.getValue(), ticker,
                new PriceDay()))
            .collect(Collectors.toList());
        System.out.println("Saving " + dayPrices.size() + " new day prices");
        priceDao.saveAll(dayPrices);
      }
      case WEEK -> {
        List<PriceWeek> weekPrices = groupedPrices.entrySet().stream()
            .map(entry -> createAggregatedPrice(entry.getKey(), entry.getValue(), ticker,
                new PriceWeek()))
            .collect(Collectors.toList());
        System.out.println("Saving " + weekPrices.size() + " new week prices");
        priceDao.saveAll(weekPrices);
      }
      case MONTH -> {
        List<PriceMonth> monthPrices = groupedPrices.entrySet().stream()
            .map(entry -> createAggregatedPrice(entry.getKey(), entry.getValue(), ticker,
                new PriceMonth()))
            .collect(Collectors.toList());
        System.out.println("Saving " + monthPrices.size() + " new month prices");
        priceDao.saveAll(monthPrices);
      }
    }
  }

  private TimeFrame getSourceTimeFrame(TimeFrame targetTimeFrame) {
    return switch (targetTimeFrame) {
      case HOUR -> TimeFrame.MIN5;
      case DAY -> TimeFrame.HOUR;
      case WEEK -> TimeFrame.DAY;
      case MONTH -> TimeFrame.DAY;  // Changed from WEEK to DAY
      default ->
          throw new IllegalArgumentException("Unsupported target time frame: " + targetTimeFrame);
    };
  }

  private List<? extends BasePrice> filterDataForMissingPeriods(
      List<? extends BasePrice> sourceData,
      List<? extends BasePrice> existingData,
      TimeFrame timeFrame) {

    Map<LocalDateTime, BasePrice> timeToPriceInDB = existingData.stream()
        .collect(Collectors.toMap(
            price -> truncateToTimeFrame(price.getId().getDate(), timeFrame),
            price -> price
        ));

    return sourceData.stream()
        .filter(price -> {
          LocalDateTime periodStart = truncateToTimeFrame(price.getId().getDate(), timeFrame);
          // Only apply market hours filter for hour aggregation and non-MOEX tickers
          if (timeFrame == TimeFrame.HOUR && !price.getId().getTicker().contains("MOEX")) {
            LocalTime time = price.getId().getDate().toLocalTime();
            return !timeToPriceInDB.containsKey(periodStart) &&
                time.isAfter(MARKET_OPEN.minusMinutes(1)) &&  // Include 9:30
                time.isBefore(MARKET_CLOSE);                   // Include up to 16:00
          }
          // For MOEX tickers or other timeframes, just check if period exists
          return !timeToPriceInDB.containsKey(periodStart);
        })
        .collect(Collectors.toList());
  }

  private LocalDateTime truncateToTimeFrame(LocalDateTime dateTime, TimeFrame timeFrame) {
    return switch (timeFrame) {
      case HOUR -> dateTime.truncatedTo(ChronoUnit.HOURS);
      case DAY -> dateTime.truncatedTo(ChronoUnit.DAYS);
      case WEEK -> dateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
          .truncatedTo(ChronoUnit.DAYS);
      case MONTH -> dateTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
      default -> throw new IllegalArgumentException("Unsupported time frame: " + timeFrame);
    };
  }

  private <T extends BasePrice> T createAggregatedPrice(
      LocalDateTime dateTime,
      List<? extends BasePrice> prices,
      String ticker,
      T aggregatedPrice) {

    PriceKey key = new PriceKey();
    key.setTicker(ticker);
    // For hour aggregation, use the time of the first price in the group
    key.setDate(prices.get(0).getId().getDate().truncatedTo(ChronoUnit.HOURS));
    aggregatedPrice.setId(key);

    aggregatedPrice.setOpen(prices.get(0).getOpen());
    aggregatedPrice.setClose(prices.get(prices.size() - 1).getClose());

    double high = prices.stream()
        .mapToDouble(BasePrice::getHigh)
        .max()
        .orElse(0.0);

    double low = prices.stream()
        .mapToDouble(BasePrice::getLow)
        .min()
        .orElse(0.0);

    long totalVolume = prices.stream()
        .mapToLong(BasePrice::getVolume)
        .sum();

    aggregatedPrice.setHigh(high);
    aggregatedPrice.setLow(low);
    aggregatedPrice.setVolume(totalVolume);

    return aggregatedPrice;
  }
}