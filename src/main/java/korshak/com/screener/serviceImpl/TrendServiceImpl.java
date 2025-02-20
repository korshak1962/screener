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
  public void calculateAndStorePriceTrendForAllTimeframes(String ticker) {
    for (TimeFrame timeFrame : TimeFrame.values()) {
      if (timeFrame != TimeFrame.MIN5) { // Skip 5-minute timeframe as it's the base
        calculateAndStorePriceTrend(ticker, timeFrame);
      }
    }
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
    //  trendRepository.deleteByIdTickerAndIdTimeframeAndIdDateBetween(
    //     ticker, timeFrame, startDate, endDate);

    List<Trend> trends = findExtremumsAndCalculateTrends(prices, ticker, timeFrame);
    return trendRepository.saveAll(trends);
  }

  private List<Trend> findExtremumsAndCalculateTrends(List<? extends BasePrice> prices,
                                                      String ticker, TimeFrame timeFrame) {
    List<Trend> trends = new ArrayList<>();

    // Track last confirmed extremums
    Double lastConfirmedMax = null;
    Double lastConfirmedMin = null;
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
      if (extremumFound && prevMax != null && prevMin != null
          && lastConfirmedMax != null && lastConfirmedMin != null) {
        trend = determineTrend(lastConfirmedMax, prevMax, lastConfirmedMin, prevMin);
      }

      // Create trend record if we found an extremum
      if (extremumFound) {
        Trend newTrend = new Trend(
            new TrendKey(ticker, current.getId().getDate(), timeFrame),
            lastConfirmedMax,  // Always store the last confirmed maximum
            lastConfirmedMin,  // Always store the last confirmed minimum
            trend
        );
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