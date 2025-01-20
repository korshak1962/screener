package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.*;
import korshak.com.screener.dao.*;
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

  private List<Trend> findExtremumsAndCalculateTrends(List<? extends BasePrice> prices, String ticker, TimeFrame timeFrame) {
    List<Trend> trends = new ArrayList<>();
    Double lastMax = prices.getFirst().getHigh();
    Double lastMin = prices.getFirst().getLow();
    Double prevMax = null;
    Double prevMin = null;

    // Skip first and last points
    for (int i = 1; i < prices.size() - 1; i++) {
      BasePrice prev = prices.get(i - 1);
      BasePrice current = prices.get(i);
      BasePrice next = prices.get(i + 1);

      // Check for local maximum
      if (current.getHigh() > prev.getHigh() && current.getHigh() > next.getHigh()) {
        int trend = 0;
        if ( prevMax != null && prevMin != null) {
          trend = determineTrend(current.getHigh(), lastMax, lastMin, prevMin);
        }

        Trend newMax = new Trend(
            new TrendKey(ticker, current.getId().getDate(), timeFrame),
            current.getHigh(),
            trend
        );
        trends.add(newMax);

        // Update max history
        prevMax = lastMax;
        lastMax = current.getHigh();
      }

      // Check for local minimum
      if (current.getLow() < prev.getLow() && current.getLow() < next.getLow()) {
        int trend = 0;
        if ( prevMax != null && prevMin != null) {
          trend = determineTrend(lastMax, prevMax, current.getLow(), prevMin);
        }
        Trend newMin = new Trend(
            new TrendKey(ticker, current.getId().getDate(), timeFrame),
            current.getLow(),
            trend  // Trend is only determined at maxima
        );
        trends.add(newMin);

        // Update min history
        prevMin = lastMin;
        lastMin = current.getLow();
      }
    }

    return trends;
  }

  private int determineTrend(double currentMax, double previousMax, double currentMin, double previousMin) {
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