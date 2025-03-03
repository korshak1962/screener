package korshak.com.screener.serviceImpl;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceMin5;
import korshak.com.screener.dao.SmaDao;
import korshak.com.screener.dao.SmaKey;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.SmaCalculationService;
import korshak.com.screener.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SmaCalculationServiceImpl implements SmaCalculationService {
  private final int TILT_PERIOD = 5;  // Default tilt period

  private final PriceDao priceDao;
  private final SmaDao smaDao;

  @Autowired
  public SmaCalculationServiceImpl(PriceDao priceDao, SmaDao smaDao) {
    this.priceDao = priceDao;
    this.smaDao = smaDao;
  }


  /**
   * Calculates incremental SMAs for all timeframes for a given ticker and length
   *
   * @param ticker Stock ticker
   * @param length SMA length
   */
  @Transactional
  public void calculateIncrementalSMAForAllTimeFrames(String ticker, int length) {
    for (TimeFrame timeFrame : TimeFrame.values()) {
      if (timeFrame != TimeFrame.MIN5) { // Skip 5-minute timeframe as it's the base
        try {
          System.out.println("Calculating incremental SMAs for " + ticker +
              " on timeframe " + timeFrame + " with length " + length);
          calculateIncrementalSMA(ticker, length, timeFrame);
        } catch (Exception e) {
          System.err.println("Error calculating SMAs for ticker " + ticker +
              " on timeframe " + timeFrame + ": " + e.getMessage());
          // Continue with other timeframes despite error
        }
      }
    }
  }

  /**
   * Calculates incremental SMAs for all tickers and timeframes for a given length
   *
   * @param length SMA length
   */
  @Transactional
  public void calculateIncrementalSMAForAllTickersAndTimeFrames(int length) {
    Set<String> tickers = priceDao.findUniqueTickers();
    int totalTickers = tickers.size();
    int currentTicker = 0;

    System.out.println("Starting incremental SMA calculation for " + totalTickers +
        " tickers with length " + length);

    for (String ticker : tickers) {
      currentTicker++;
      try {
        System.out.println("Processing ticker " + ticker + " (" +
            currentTicker + "/" + totalTickers + ")");
        calculateIncrementalSMAForAllTimeFrames(ticker, length);
      } catch (Exception e) {
        System.err.println("Error processing ticker " + ticker + ": " + e.getMessage());
        e.printStackTrace();
        // Continue with next ticker despite error
      }
    }

    System.out.println("Completed incremental SMA calculation for all tickers");
  }

  /**
   * Calculates SMA and tilt only for new prices, utilizing existing SMA values
   *
   * @param ticker    Stock ticker
   * @param length    SMA length
   * @param timeFrame Time frame for calculation
   * @return List of newly calculated SMAs
   */
  @Transactional
  public List<? extends BaseSma> calculateIncrementalSMA(
      String ticker, int length, TimeFrame timeFrame) {
    // Get all prices and existing SMAs - assuming they're ordered by date
    List<? extends BasePrice> prices = priceDao.findAllByTicker(ticker, timeFrame);
    List<? extends BaseSma> existingSmas = smaDao.findAllByTicker(ticker, timeFrame, length);

    if (prices.isEmpty()) {
      return List.of();
    }

    // Find starting point for new calculations
    LocalDateTime startDate = existingSmas.isEmpty() ?
        prices.get(0).getId().getDate() :
        existingSmas.get(existingSmas.size() - 1).getId().getDate();

    // Find index of first price after startDate using binary search
    int newPricesStartIndex = Collections.binarySearch(prices,
        new PriceMin5(new PriceKey(ticker, startDate), 0, 0, 0, 0, 0),
        (p1, p2) -> p1.getId().getDate().compareTo(p2.getId().getDate()));

    // If exact match not found, get insertion point
    if (newPricesStartIndex < 0) {
      newPricesStartIndex = -(newPricesStartIndex + 1);
    }

    // No new prices to process
    if (newPricesStartIndex >= prices.size()) {
      return List.of();
    }

    // Calculate start index for previous prices needed for SMA window
    int previousPricesStartIndex = Math.max(0, newPricesStartIndex - (length - 1));
    List<? extends BasePrice> previousPrices =
        prices.subList(previousPricesStartIndex, newPricesStartIndex);
    List<? extends BasePrice> pricesToProcess = prices.subList(newPricesStartIndex, prices.size());

    // Get previous SMAs for tilt calculation using binary search if we have existing SMAs
    List<? extends BaseSma> previousSmas = List.of();
    if (!existingSmas.isEmpty()) {
      int tiltStartIndex = Math.max(0, existingSmas.size() - (TILT_PERIOD - 1));
      previousSmas = existingSmas.subList(tiltStartIndex, existingSmas.size());
    }

    List<BaseSma> newSmas = new ArrayList<>();
    double sum = 0;

    // Calculate initial sum using previous prices
    for (BasePrice price : previousPrices) {
      sum += price.getClose();
    }

    // Maintain running window for SMA calculation
    List<BasePrice> window = new ArrayList<>(previousPrices);
    List<BaseSma> tiltWindow = new ArrayList<>(previousSmas);

    // Calculate new SMAs
    for (BasePrice currentPrice : pricesToProcess) {
      window.add(currentPrice);
      sum += currentPrice.getClose();

      if (window.size() > length) {
        sum -= window.get(0).getClose();
        window.remove(0);
      }

      if (window.size() == length) {
        BaseSma sma = Utils.getBaseSma(timeFrame);
        sma.setId(new SmaKey(ticker, currentPrice.getId().getDate(), length));
        sma.setValue(sum / length);

        // Update tilt window and calculate tilt
        tiltWindow.add(sma);
        if (tiltWindow.size() > TILT_PERIOD) {
          tiltWindow.remove(0);
        }
        if (tiltWindow.size() == TILT_PERIOD) {
          double tilt = Utils.calculateTilt(tiltWindow);
          sma.setTilt(tilt);
          sma.setYield(Utils.calculateYield(timeFrame, tilt));
        }

        newSmas.add(sma);
      }
    }

    // Save new SMAs
    if (!newSmas.isEmpty()) {
      smaDao.saveAll(newSmas, timeFrame);
    }

    return newSmas;
  }

  @Transactional
  public void calculateSMA(
      String ticker,
      int length,
      LocalDateTime startDate,
      LocalDateTime endDate,
      TimeFrame timeFrame) {

    // Get existing SMA data
    List<? extends BaseSma> existingSmas = smaDao.findByDateRangeOrderByIdDateAsc(
        ticker, startDate, endDate, timeFrame, length);

    // Get price data
    List<? extends BasePrice> prices = priceDao.findByDateRange(
        ticker, startDate, endDate, timeFrame);

    if (prices.isEmpty()) {
      return;
    }

    // Get date ranges that need calculation
    LocalDateTime firstDate = prices.get(0).getId().getDate();
    LocalDateTime lastDate = prices.get(prices.size() - 1).getId().getDate();

    // Filter price data for missing periods
    List<? extends BasePrice> pricesToProcess = filterDataForMissingPeriods(
        prices, existingSmas, firstDate, lastDate);

    if (pricesToProcess.isEmpty()) {
      System.out.println("No new SMAs to calculate for " + ticker + " at " + timeFrame + " level");
      return;
    }

    calculateAndSaveSMA(pricesToProcess, ticker, length, timeFrame);
  }

  private List<? extends BasePrice> filterDataForMissingPeriods(
      List<? extends BasePrice> prices,
      List<? extends BaseSma> existingSmas,
      LocalDateTime firstDate,
      LocalDateTime lastDate) {

    // Create a map of existing SMAs by date
    Map<LocalDateTime, BaseSma> existingSmaDates = existingSmas.stream()
        .collect(Collectors.toMap(
            sma -> sma.getId().getDate(),
            sma -> sma
        ));

    // Filter prices to only include those without SMAs
    return prices.stream()
        .filter(price -> !existingSmaDates.containsKey(price.getId().getDate()))
        .collect(Collectors.toList());
  }

  private List<BaseSma> calculateAndSaveSMA(
      List<? extends BasePrice> prices,
      String ticker,
      int length,
      TimeFrame timeFrame) {

    if (prices.size() < length) {
      return null;
    }

    List<BaseSma> smaResults = new ArrayList<>();
    double sum = 0;

    // Calculate initial sum
    for (int i = 0; i < length; i++) {
      sum += prices.get(i).getClose();
    }

    // Calculate SMAs and maintain a window for tilt calculation
    List<BaseSma> tiltWindow = new ArrayList<>();
    for (int i = length - 1; i < prices.size(); i++) {
      BaseSma sma = Utils.getBaseSma(timeFrame);
      SmaKey smaKey = new SmaKey(
          ticker,
          prices.get(i).getId().getDate(),
          length
      );
      sma.setId(smaKey);
      sma.setValue(sum / length);
      smaResults.add(sma);
      tiltWindow.add(sma);

      // Keep tilt window size = TILT_PERIOD
      if (tiltWindow.size() > TILT_PERIOD) {
        tiltWindow.remove(0);
      }

      // Calculate tilt when we have enough data points
      if (tiltWindow.size() == TILT_PERIOD) {
        double tilt = Utils.calculateTilt(tiltWindow);
        sma.setTilt(tilt);
        sma.setYield(Utils.calculateYield(timeFrame, tilt));
      }

      if (i < prices.size() - 1) {
        sum = sum - prices.get(i - length + 1).getClose()
            + prices.get(i + 1).getClose();
      }
    }

    if (!smaResults.isEmpty()) {
      smaDao.saveAll(smaResults, timeFrame);
    }
    return smaResults;
  }

  @Override
  public void calculateSMA(String ticker, int length, TimeFrame timeFrame) {
    List<? extends BasePrice> prices = priceDao.findAllByTicker(ticker, timeFrame);
    if (prices.isEmpty()) {
      throw new RuntimeException(
          "prices for timeframe = " + timeFrame + " for ticker = " + ticker + " not found");
    }
    calculateAndSaveSMA(prices, ticker, length, timeFrame);
  }

  @Override
  public void calculateSMAForAllTimeFrame(String ticker, int length) {
    for (TimeFrame timeFrame : TimeFrame.values()) {
      if (timeFrame != TimeFrame.MIN5) {
        calculateSMA(ticker, length, timeFrame);
      }
    }
  }

  @Override
  public void calculateSMAForAllTimeFrameAndTickers(int length) {
    Set<String> tickers = priceDao.findUniqueTickers();
    for (String ticker : tickers) {
      System.out.println("Calculating SMAs for " + ticker + " length = " + length);
      calculateSMAForAllTimeFrame(ticker, length);
    }
  }


}