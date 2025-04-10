package korshak.com.screener.serviceImpl.calc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.SmaDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.utils.Utils;
import org.springframework.stereotype.Service;

@Service
public class FuturePriceByTiltCalculator {
  private static final int TILT_PERIOD = 5;
  private static final double INITIAL_STEP = 0.02; // 1%
  private static final double PRECISION = 0.001;
  private static final int MAX_ITERATIONS = 100;

  private final PriceDao priceDao;
  private final SmaDao smaDao;

  public FuturePriceByTiltCalculator(PriceDao priceDao, SmaDao smaDao) {
    this.priceDao = priceDao;
    this.smaDao = smaDao;
  }

  public double calculatePrice(String ticker, TimeFrame timeFrame, int smaLength,
                               double targetTilt) {
    // Use current time as end date
    LocalDateTime endDate = LocalDateTime.now();

    // Calculate start date based on the end date to get enough data
    LocalDateTime startDate = calculateStartDate(endDate, timeFrame, smaLength + TILT_PERIOD);

    // Get just enough prices and SMAs for the calculation
    List<? extends BasePrice> prices =
        priceDao.findByDateRange(ticker, startDate, endDate, timeFrame);
    List<? extends BaseSma> smas =
        smaDao.findByDateRangeOrderByIdDateAsc(ticker, startDate, endDate, timeFrame, smaLength);

    if (prices.size() < smaLength || smas.size() < TILT_PERIOD) {
      System.out.println("Insufficient historical data for ticker " + ticker);
      return 0.0;
    }

    // Get the required data for calculation
    List<? extends BasePrice> latestPrices =
        prices.subList(prices.size() - (smaLength - 1), prices.size());
    List<? extends BaseSma> latestSmas = smas.subList(smas.size() - (TILT_PERIOD - 1), smas.size());

    return calculatePriceFromData(latestPrices, latestSmas, smaLength, targetTilt);
  }

  /**
   * Calculates the price that would result in the target tilt using binary search algorithm.
   *
   * @param ticker     Stock ticker
   * @param timeFrame  Time frame (DAY, WEEK, etc.)
   * @param smaLength  SMA length/period
   * @param targetTilt The target tilt value to achieve
   * @return The calculated price
   */
  public double calculatePriceBinary(String ticker, TimeFrame timeFrame, int smaLength,
                                     double targetTilt) {
    // Use current time as end date
    LocalDateTime endDate = LocalDateTime.now();

    // Calculate start date based on the end date to get enough data
    LocalDateTime startDate = calculateStartDate(endDate, timeFrame, smaLength + TILT_PERIOD);

    // Get just enough prices and SMAs for the calculation
    List<? extends BasePrice> prices =
        priceDao.findByDateRange(ticker, startDate, endDate, timeFrame);
    List<? extends BaseSma> smas =
        smaDao.findByDateRangeOrderByIdDateAsc(ticker, startDate, endDate, timeFrame, smaLength);

    if (prices.size() < smaLength || smas.size() < TILT_PERIOD) {
      System.out.println("Insufficient historical data for ticker " + ticker);
      return 0.0;
    }

    // Get the required data for calculation
    List<? extends BasePrice> latestPrices =
        prices.subList(prices.size() - (smaLength - 1), prices.size());
    List<? extends BaseSma> latestSmas = smas.subList(smas.size() - (TILT_PERIOD - 1), smas.size());

    return calculatePriceFromDataBinary(latestPrices, latestSmas, smaLength, targetTilt);
  }

  /**
   * Calculates the start date for data retrieval based on end date and required length
   *
   * @param endDate   The end date
   * @param timeFrame The time frame
   * @param length    The SMA length
   * @return The calculated start date
   */
  private LocalDateTime calculateStartDate(LocalDateTime endDate, TimeFrame timeFrame, int length) {
    int multiplier = 2 * length;
    return switch (timeFrame) {
      case MIN5 -> endDate.minusMinutes(multiplier * 5L);
      case HOUR -> endDate.minusHours(multiplier);
      case DAY -> endDate.minusDays(multiplier);
      case WEEK -> endDate.minusDays(multiplier * 7L);
      case MONTH -> endDate.minusDays(multiplier * 30L);
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
      throw new RuntimeException(
          "Failed to converge to target tilt within " + MAX_ITERATIONS + " iterations");
    }

    return calcPrice;
  }

  /**
   * Calculates the price that would result in the target tilt using binary search.
   * Uses fixed bounds from 0 to 2x last price.
   */
  public double calculatePriceFromDataBinary(List<? extends BasePrice> prices,
                                             List<? extends BaseSma> previousSmas,
                                             int smaLength,
                                             double targetTilt) {
    validateInput(prices, previousSmas, smaLength);

    double lastPrice = prices.get(prices.size() - 1).getClose();

    // Set fixed bounds: 0 to 2x last price
    double lowerPrice = 0.0;
    double upperPrice = lastPrice * 2.0;

    // Get tilts at min and max prices
    double lowerTilt = calculateTiltWithPrice(prices, previousSmas, smaLength, lowerPrice);
    double upperTilt = calculateTiltWithPrice(prices, previousSmas, smaLength, upperPrice);

/*    System.out.println("Binary search setup:");
    System.out.println("- Last price: " + lastPrice);
    System.out.println("- Price bounds: [" + lowerPrice + ", " + upperPrice + "]");
    System.out.println("- Tilt bounds: [" + lowerTilt + ", " + upperTilt + "]");
    System.out.println("- Target tilt: " + targetTilt);

 */

    // Determine relationship direction (direct or inverse)
    boolean isDirectRelationship = lowerTilt < upperTilt;

    // Check if target tilt is within bounds
    if ((isDirectRelationship && (targetTilt < lowerTilt || targetTilt > upperTilt)) ||
        (!isDirectRelationship && (targetTilt > lowerTilt || targetTilt < upperTilt))) {
      System.out.println("Warning: Target tilt " + targetTilt + " is outside the bounds [" +
          lowerTilt + ", " + upperTilt + "]");
      return -1.0; // Indicate that no solution is possible within bounds
    }

    // Quick check for exact matches at bounds
    if (Math.abs(lowerTilt - targetTilt) < PRECISION) {
      return lowerPrice;
    }
    if (Math.abs(upperTilt - targetTilt) < PRECISION) {
      return upperPrice;
    }

    // Binary search between bounds
    double calcPrice = 0.0;
    double currentTilt = 0.0;
    int iterations = 0;

    while (iterations < MAX_ITERATIONS) {
      iterations++;

      // Calculate midpoint price
      calcPrice = (lowerPrice + upperPrice) / 2.0;
      currentTilt = calculateTiltWithPrice(prices, previousSmas, smaLength, calcPrice);

      /* if (iterations % 5 == 0) {
        System.out.println("Iteration " + iterations + ": price = " + calcPrice +
            ", tilt = " + currentTilt + ", bounds = [" +
            lowerPrice + ", " + upperPrice + "]");
      }*/

      // If we're close enough to target tilt, we're done
      if (Math.abs(currentTilt - targetTilt) < PRECISION) {
        break;
      }

      // Update bounds based on current tilt and relationship direction
      if (isDirectRelationship) {
        // Direct relationship: price ↑ => tilt ↑
        if (currentTilt < targetTilt) {
          lowerPrice = calcPrice; // Need higher price
        } else {
          upperPrice = calcPrice; // Need lower price
        }
      } else {
        // Inverse relationship: price ↑ => tilt ↓
        if (currentTilt > targetTilt) {
          lowerPrice = calcPrice; // Need higher price
        } else {
          upperPrice = calcPrice; // Need lower price
        }
      }

      // If price bounds are very close, we're done
      if (Math.abs(upperPrice - lowerPrice) < PRECISION) {
        break;
      }
    }

    /* System.out.println("Final binary calculation: price = " + calcPrice + ", tilt = " + currentTilt +
        " (after " + iterations + " iterations)");
     */

    if (iterations == MAX_ITERATIONS) {
      System.out.println("Warning: Binary search did not fully converge");
    }

    return calcPrice;
  }

  private void validateInput(List<? extends BasePrice> prices,
                             List<? extends BaseSma> previousSmas,
                             int smaLength) {
    if (prices.size() != smaLength - 1) {
      throw new IllegalArgumentException(
          "Expected " + (smaLength - 1) + " prices, got " + prices.size());
    }
    if (previousSmas.size() != TILT_PERIOD - 1) {
      throw new IllegalArgumentException(
          "Expected " + (TILT_PERIOD - 1) + " SMAs, got " + previousSmas.size());
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
    BaseSma newSmaPeriod = Utils.getBaseSma(
        previousSmas.get(0).getId().getDate().toLocalTime().getHour() == 0 ? TimeFrame.DAY :
            TimeFrame.HOUR);
    newSmaPeriod.setValue(newSma);
    allSmas.add(newSmaPeriod);

    return Utils.calculateTilt(allSmas);
  }
}