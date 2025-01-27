package korshak.com.screener.serviceImpl;

import korshak.com.screener.dao.*;
import korshak.com.screener.utils.Utils;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;

@Service
public class PriceTiltCalculator {
  private static final int TILT_PERIOD = 5;
  private static final double INITIAL_STEP = 0.01; // 1%
  private static final double PRECISION = 0.001;
  private static final int MAX_ITERATIONS = 100;

  private final PriceDao priceDao;
  private final SmaDao smaDao;

  public PriceTiltCalculator(PriceDao priceDao, SmaDao smaDao) {
    this.priceDao = priceDao;
    this.smaDao = smaDao;
  }

  public double calculatePrice(String ticker, TimeFrame timeFrame, int smaLength, double targetTilt) {
    LocalDateTime endDate = LocalDateTime.now();
    LocalDateTime startDate = calculateStartDate(endDate, timeFrame, smaLength);

    List<? extends BasePrice> prices = priceDao.findByDateRange(ticker, startDate, endDate, timeFrame);
    List<? extends BaseSma> smas = smaDao.findByDateRangeOrderByIdDateAsc(ticker, startDate, endDate, timeFrame, smaLength);

    if (prices.isEmpty() || smas.isEmpty()) {
      throw new IllegalStateException("No data found for ticker " + ticker);
    }

    List<? extends BasePrice> latestPrices = prices.subList(prices.size() - (smaLength - 1), prices.size());
    List<? extends BaseSma> latestSmas = smas.subList(smas.size() - (TILT_PERIOD - 1), smas.size());

    return calculatePriceFromData(latestPrices, latestSmas, smaLength, targetTilt);
  }

  private LocalDateTime calculateStartDate(LocalDateTime endDate, TimeFrame timeFrame, int length) {
    int multiplier = 2 * length;
    return switch (timeFrame) {
      case MIN5 -> endDate.minus(multiplier * 5, ChronoUnit.MINUTES);
      case HOUR -> endDate.minus(multiplier, ChronoUnit.HOURS);
      case DAY -> endDate.minus(multiplier, ChronoUnit.DAYS);
      case WEEK -> endDate.minus(multiplier * 7, ChronoUnit.DAYS);
      case MONTH -> endDate.minus(multiplier * 30, ChronoUnit.DAYS);
    };
  }

  public double calculatePriceFromData(List<? extends BasePrice> prices,
                                       List<? extends BaseSma> previousSmas,
                                       int smaLength,
                                       double targetTilt) {
    validateInput(prices, previousSmas, smaLength);

    double lastPrice = prices.get(prices.size() - 1).getClose();
    double step = lastPrice * INITIAL_STEP;
    double calcPrice = lastPrice;
    int iterations = 0;

    double currentTilt = calculateTiltWithPrice(prices, previousSmas, smaLength, calcPrice);

    while (Math.abs(currentTilt - targetTilt) > PRECISION && iterations < MAX_ITERATIONS) {
      if (currentTilt > targetTilt) {
        calcPrice -= step;
      } else {
        calcPrice += step;
      }

      currentTilt = calculateTiltWithPrice(prices, previousSmas, smaLength, calcPrice);

      // Reduce step if we're oscillating
      if (iterations % 6 == 0) {
        step *= 0.5;
      }

      iterations++;
    }

    if (iterations == MAX_ITERATIONS) {
      throw new RuntimeException("Failed to converge to target tilt within " + MAX_ITERATIONS + " iterations");
    }

    return calcPrice;
  }

  private void validateInput(List<? extends BasePrice> prices,
                             List<? extends BaseSma> previousSmas,
                             int smaLength) {
    if (prices.size() != smaLength - 1) {
      throw new IllegalArgumentException("Expected " + (smaLength - 1) + " prices, got " + prices.size());
    }
    if (previousSmas.size() != TILT_PERIOD - 1) {
      throw new IllegalArgumentException("Expected " + (TILT_PERIOD - 1) + " SMAs, got " + previousSmas.size());
    }
  }

  private double calculateTiltWithPrice(List<? extends BasePrice> prices,
                                        List<? extends BaseSma> previousSmas,
                                        int smaLength,
                                        double calcPrice) {
    // Calculate new SMA with the test price
    double sum = prices.stream().mapToDouble(BasePrice::getClose).sum() + calcPrice;
    double newSma = sum / smaLength;

    // Prepare the list of SMAs for tilt calculation
    List<BaseSma> allSmas = new ArrayList<>();
    for (BaseSma sma : previousSmas) {
      allSmas.add(sma);
    }

    // Add new SMA value
    BaseSma newSmaPeriod = Utils.getBaseSma(previousSmas.get(0).getId().getDate().toLocalTime().getHour() == 0 ? TimeFrame.DAY : TimeFrame.HOUR);
    newSmaPeriod.setValue(newSma);
    allSmas.add(newSmaPeriod);

    return Utils.calculateTilt(allSmas);
  }
}