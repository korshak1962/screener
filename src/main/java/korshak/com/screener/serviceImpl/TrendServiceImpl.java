package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.Trend;
import korshak.com.screener.dao.TrendKey;
import korshak.com.screener.dao.TrendRepository;
import korshak.com.screener.service.TrendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrendServiceImpl implements TrendService {
  private final PriceDao priceDao;
  private final TrendRepository trendRepository;

  @Autowired
  public TrendServiceImpl(PriceDao priceDao, TrendRepository trendRepository) {
    this.priceDao = priceDao;
    this.trendRepository = trendRepository;
  }

  @Override
  @Transactional
  public Trend findLatestTrendBeforeDate(String ticker, TimeFrame timeFrame, LocalDateTime date) {
    return trendRepository.findTopByIdTickerAndIdTimeframeAndIdDateLessThanEqualOrderByIdDateDesc(
        ticker, timeFrame.toString().trim(), date);
  }

  @Override
  @Transactional
  public void calculateAndStorePriceTrendForAllTimeframes(String ticker,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate) {
    for (TimeFrame timeFrame : TimeFrame.values()) {
      if (timeFrame != TimeFrame.MIN5) { // Skip 5-minute timeframe as it's the base
        calculateAndStorePriceTrend(ticker, timeFrame,
            startDate, endDate);
      }
    }
  }

  @Override
  public Trend findLatestTrendChangeBefore(String ticker, TimeFrame timeFrame, Trend latestTrend) {

    if (latestTrend == null) {
      return null; // No trend found before the date
    }

    // Get the trend value of the latest trend
    int latestTrendValue = latestTrend.getTrend();

    // Query for the most recent trend with a different trend value
    // This requires a custom query in the repository
    return trendRepository.findLatestTrendChange(
        ticker,
        timeFrame.toString(),
        latestTrend.getId().getDate(),
        latestTrendValue
    );
  }

  @Override
  @Transactional
  public List<Trend> calculateAndStorePriceTrend(String ticker, TimeFrame timeFrame) {
    List<? extends BasePrice> prices = priceDao.findAllByTicker(ticker, timeFrame);

    if (prices.size() < 3) {
      return Collections.emptyList();
    }

    // Delete existing trends
    LocalDateTime startDate = prices.get(0).getId().getDate();
    LocalDateTime endDate = prices.get(prices.size() - 1).getId().getDate();
    trendRepository.deleteByIdTickerAndIdTimeframeAndIdDateBetween(
        ticker, timeFrame, startDate, endDate);

    List<Trend> trends = findExtremumsAndCalculateTrends(prices, ticker, timeFrame);
    return trendRepository.saveAll(trends);
  }

  @Override
  @Transactional
  public void calculateAndStorePriceTrendForAllTimeframes(String ticker) {
    for (TimeFrame timeFrame : TimeFrame.values()) {
      if (timeFrame != TimeFrame.MIN5) { // Skip 5-minute timeframe as it's the base
        calculateAndStorePriceTrend(ticker, timeFrame);
      }
    }
  }

  @Transactional
  @Override
  public List<Trend> calculateAndStorePriceTrend(String ticker, TimeFrame timeFrame,
                                                 LocalDateTime startDate, LocalDateTime endDate) {
    // First check if data already exists for the entire requested period
    List<Trend> existingTrends =
        trendRepository.findByIdTickerAndIdTimeframeAndIdDateBetweenOrderByIdDateAsc(
            ticker, timeFrame, startDate, endDate);
    // Get all price data needed for the calculation
    List<? extends BasePrice> prices =
        priceDao.findByDateRange(ticker, startDate, endDate, timeFrame);
    if (prices.size() < 3) {
      return Collections.emptyList();
    }

    // If there's existing trend data covering the entire period, return it
    if (!existingTrends.isEmpty()) {
      LocalDateTime firstTrendDate = existingTrends.get(0).getId().getDate();
      LocalDateTime lastTrendDate = existingTrends.get(existingTrends.size() - 1).getId().getDate();

      if (!firstTrendDate.isAfter(startDate) && !lastTrendDate.isBefore(endDate)) {
        return existingTrends;
      }
    }

    // Find the latest date for which trend data exists
    LocalDateTime latestExistingTrendDate = null;
    if (!existingTrends.isEmpty()) {
      latestExistingTrendDate = existingTrends.get(existingTrends.size() - 1).getId().getDate();
    }

    // If we have existing trend data, we only need to calculate trends after the latest date
    if (latestExistingTrendDate != null) {
      // Filter prices to only include those after the latest trend date
      List<? extends BasePrice> newPrices = new ArrayList<>();
      for (BasePrice price : prices) {
        if (price.getId().getDate().isAfter(latestExistingTrendDate)) {
          ((List<BasePrice>) newPrices).add(price);
        }
      }

      // Add a few prices before the cutoff to ensure proper trend calculation
      int lookbackCount = 2; // Need at least 2 previous points for trend calculation
      int startIndex = 0;
      for (int i = 0; i < prices.size(); i++) {
        if (prices.get(i).getId().getDate().isAfter(latestExistingTrendDate)) {
          startIndex = Math.max(0, i - lookbackCount);
          break;
        }
      }

      List<? extends BasePrice> pricesToProcess = prices.subList(startIndex, prices.size());

      // Calculate trends for new data
      List<Trend> newTrends = findExtremumsAndCalculateTrends(pricesToProcess, ticker, timeFrame);

      // Filter out trends with dates that already exist
      List<Trend> trendsToSave = new ArrayList<>();
      for (Trend trend : newTrends) {
        if (trend.getId().getDate().isAfter(latestExistingTrendDate)) {
          trendsToSave.add(trend);
        }
      }

      // Save new trends
      if (!trendsToSave.isEmpty()) {
        return trendRepository.saveAll(trendsToSave);
      } else {
        return existingTrends;
      }
    } else {
      // No existing trend data, calculate for the entire period
      List<Trend> trends = findExtremumsAndCalculateTrends(prices, ticker, timeFrame);
      return trendRepository.saveAll(trends);
    }
  }

  @Override
  public List<Trend> findByIdTickerAndIdTimeframeAndIdDateBetweenOrderByIdDateAsc(String ticker,
                                                                                  TimeFrame timeframe,
                                                                                  LocalDateTime startDate,
                                                                                  LocalDateTime endDate) {
    return trendRepository.findByIdTickerAndIdTimeframeAndIdDateBetweenOrderByIdDateAsc(ticker,
        timeframe,
        startDate,
        endDate);
  }

  private List<Trend> findExtremumsAndCalculateTrends(List<? extends BasePrice> prices,
                                                      String ticker, TimeFrame timeFrame) {
    List<Trend> trends = new ArrayList<>();

    // Track last confirmed extremums
    double lastConfirmedMax = -Double.MAX_VALUE;
    double lastConfirmedMin = Double.MAX_VALUE;
    LocalDateTime lastMaxDate = null;
    LocalDateTime lastMinDate = null;

    // Previous values for trend determination
    Double prevMax = null;
    Double prevMin = null;

    // Skip first and last points
    for (int i = 1; i < prices.size() - 1; i++) {
      BasePrice prev = prices.get(i - 1);
      BasePrice current = prices.get(i);
      BasePrice next = prices.get(i + 1);

      boolean extremumFound = false;
      int trend = 0;

      // Check for local maximum
      if (current.getHigh() > prev.getHigh() && current.getHigh() > next.getHigh()) {
        prevMax = lastConfirmedMax;
        lastConfirmedMax = current.getHigh();
        lastMaxDate = current.getId().getDate();
        extremumFound = true;
      }

      // Check for local minimum
      if (current.getLow() < prev.getLow() && current.getLow() < next.getLow()) {
        prevMin = lastConfirmedMin;
        lastConfirmedMin = current.getLow();
        lastMinDate = current.getId().getDate();
        extremumFound = true;
      }

      // If we found an extremum and have enough history, determine trend
      if (extremumFound && prevMax != null && prevMin != null) {
        trend = determineTrend(lastConfirmedMax, prevMax, lastConfirmedMin, prevMin);
      }
      // Create trend record if we found an extremum
      Trend newTrend = null;
      if (extremumFound) {
        newTrend = new Trend(
            new TrendKey(ticker, current.getId().getDate(), timeFrame),
            lastConfirmedMax,  // Always store the last confirmed maximum
            lastConfirmedMin,  // Always store the last confirmed minimum
            trend
        );
      }
      if (trend == -1 && current.getClose() > lastConfirmedMax) {
        newTrend = new Trend(
            new TrendKey(ticker, current.getId().getDate(), timeFrame),
            current.getClose(),  // Always store the last confirmed maximum
            lastConfirmedMin,  // Always store the last confirmed minimum
            0
        );
      }
      if (trend == 1 && current.getClose() < lastConfirmedMin) {
        newTrend = new Trend(
            new TrendKey(ticker, current.getId().getDate(), timeFrame),
            lastConfirmedMax,  // Always store the last confirmed maximum
            current.getClose(),  // Always store the last confirmed minimum
            0
        );
      }
      if (newTrend != null) {
        trends.add(newTrend);
      }
    }
    return trends;
  }

  private int determineTrend(double currentMax, double previousMax, double currentMin,
                             double previousMin) {
    // Uptrend: Both maximum and minimum are higher
    if (currentMax > previousMax && currentMin > previousMin) {
      return 1;
    }
    // Downtrend: Both maximum and minimum are lower
    if (currentMax < previousMax && currentMin < previousMin) {
      return -1;
    }
    return 0;
  }
}