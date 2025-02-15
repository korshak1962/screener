package korshak.com.screener.serviceImpl;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseRsi;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceDay;
import korshak.com.screener.dao.PriceHour;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceMin5;
import korshak.com.screener.dao.PriceMonth;
import korshak.com.screener.dao.PriceWeek;
import korshak.com.screener.dao.RsiDay;
import korshak.com.screener.dao.RsiDayRepository;
import korshak.com.screener.dao.RsiHour;
import korshak.com.screener.dao.RsiHourRepository;
import korshak.com.screener.dao.RsiKey;
import korshak.com.screener.dao.RsiMin5;
import korshak.com.screener.dao.RsiMin5Repository;
import korshak.com.screener.dao.RsiMonth;
import korshak.com.screener.dao.RsiMonthRepository;
import korshak.com.screener.dao.RsiRepository;
import korshak.com.screener.dao.RsiWeek;
import korshak.com.screener.dao.RsiWeekRepository;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.RsiService;
import korshak.com.screener.utils.Utils;
import org.springframework.stereotype.Service;

@Service
public class RsiServiceImpl implements RsiService {
  private static final int TILT_PERIOD = 5;
  private final PriceDao priceDao;
  private final RsiMin5Repository min5Repository;
  private final RsiHourRepository hourRepository;
  private final RsiDayRepository dayRepository;
  private final RsiWeekRepository weekRepository;
  private final RsiMonthRepository monthRepository;

  public RsiServiceImpl(PriceDao priceDao,
                        RsiMin5Repository min5Repository,
                        RsiHourRepository hourRepository,
                        RsiDayRepository dayRepository,
                        RsiWeekRepository weekRepository,
                        RsiMonthRepository monthRepository) {
    this.priceDao = priceDao;
    this.min5Repository = min5Repository;
    this.hourRepository = hourRepository;
    this.dayRepository = dayRepository;
    this.weekRepository = weekRepository;
    this.monthRepository = monthRepository;
  }

  @Override
  @Transactional
  public void calculateRsi(String ticker, int length, TimeFrame timeFrame) {
    List<? extends BasePrice> prices = priceDao.findAllByTicker(ticker, timeFrame);
    if (prices.isEmpty()) {
      throw new RuntimeException(
          "No prices found for ticker " + ticker + " and timeframe " + timeFrame);
    }
    calculateAndSaveRsi(prices, ticker, length, timeFrame);
  }

  @Override
  public void calculateRsi(String ticker, int length, LocalDateTime startDate,
                           LocalDateTime endDate, TimeFrame timeFrame) {
    List<? extends BasePrice> prices =
        priceDao.findByDateRange(ticker, startDate, endDate, timeFrame);
    if (prices.isEmpty()) {
      throw new RuntimeException(
          "No prices found for ticker " + ticker + " and timeframe " + timeFrame);
    }
    calculateAndSaveRsi(prices, ticker, length, timeFrame);
  }

  private void calculateAndSaveRsi(List<? extends BasePrice> prices, String ticker, int length,
                                   TimeFrame timeFrame) {
    if (prices.size() < length + 1) {
      return;
    }

    List<BaseRsi> rsiResults = new ArrayList<>();
    List<Double> gains = new ArrayList<>();
    List<Double> losses = new ArrayList<>();

    // Calculate initial gains and losses
    for (int i = 1; i < length + 1; i++) {
      double change = prices.get(i).getClose() - prices.get(i - 1).getClose();
      gains.add(Math.max(change, 0));
      losses.add(Math.max(-change, 0));
    }

    // Calculate first RSI
    double avgGain = gains.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    double avgLoss = losses.stream().mapToDouble(Double::doubleValue).average().orElse(0);

    List<BaseRsi> tiltWindow = new ArrayList<>();

    for (int i = length; i < prices.size(); i++) {
      double rs = avgLoss == 0 ? Double.POSITIVE_INFINITY : avgGain / avgLoss;
      double rsiValue;
      if (avgGain == 0 && avgLoss == 0) {
        rsiValue = 50.0; // Neutral when no movement
      } else if (avgLoss == 0) {
        rsiValue = 100.0; // All gains
      } else if (avgGain == 0) {
        rsiValue = 0.0;  // All losses
      } else {
        rsiValue = 100.0 - (100.0 / (1.0 + rs));
      }

      BaseRsi rsi = createRsi(timeFrame);
      RsiKey rsiKey = new RsiKey(ticker, prices.get(i).getId().getDate(), length);
      rsi.setId(rsiKey);
      rsi.setValue(rsiValue);

      // Update tilt calculation
      tiltWindow.add(rsi);
      if (tiltWindow.size() > TILT_PERIOD) {
        tiltWindow.remove(0);
      }
      if (tiltWindow.size() == TILT_PERIOD) {
        double tilt = Utils.calculateTilt(tiltWindow);
        rsi.setTilt(tilt);
      }

      rsiResults.add(rsi);

      // Calculate next period gains/losses using smoothing
      if (i < prices.size() - 1) {
        double change = prices.get(i + 1).getClose() - prices.get(i).getClose();
        double currentGain = Math.max(change, 0);
        double currentLoss = Math.max(-change, 0);

        avgGain = ((avgGain * (length - 1)) + currentGain) / length;
        avgLoss = ((avgLoss * (length - 1)) + currentLoss) / length;
      }
    }

    // Save results
    saveRsi(rsiResults, timeFrame);
  }

  @Override
  @Transactional
  public List<? extends BaseRsi> calculateIncrementalRsi(String ticker, int length,
                                                         TimeFrame timeFrame) {
    // Get all prices and existing RSIs - assuming they're ordered by date
    List<? extends BasePrice> prices = priceDao.findAllByTicker(ticker, timeFrame);
    List<? extends BaseRsi> existingRsis = getRsiRepository(timeFrame)
        .findByIdTickerAndIdLengthOrderByIdDateAsc(ticker, length);

    if (prices.isEmpty()) {
      return List.of();
    }

    // Find starting point for new calculations
    LocalDateTime startDate = existingRsis.isEmpty() ?
        prices.get(0).getId().getDate() :
        existingRsis.get(existingRsis.size() - 1).getId().getDate();

    // Find index of first price after startDate
    int newPricesStartIndex = Collections.binarySearch(prices,
        createBasePrice(timeFrame, new PriceKey(ticker, startDate)),
        (p1, p2) -> p1.getId().getDate().compareTo(p2.getId().getDate()));

    // If exact match not found, get insertion point
    if (newPricesStartIndex < 0) {
      newPricesStartIndex = -(newPricesStartIndex + 1);
    }

    // No new prices to process
    if (newPricesStartIndex >= prices.size()) {
      return List.of();
    }

    // Calculate start index for previous prices needed for RSI window
    int previousPricesStartIndex = Math.max(0, newPricesStartIndex - length);
    List<? extends BasePrice> previousPrices =
        prices.subList(previousPricesStartIndex, newPricesStartIndex);
    List<? extends BasePrice> pricesToProcess = prices.subList(newPricesStartIndex, prices.size());

    // Get previous RSIs for tilt calculation
    List<? extends BaseRsi> previousRsis = List.of();
    if (!existingRsis.isEmpty()) {
      int tiltStartIndex = Math.max(0, existingRsis.size() - (TILT_PERIOD - 1));
      previousRsis = existingRsis.subList(tiltStartIndex, existingRsis.size());
    }

    List<BaseRsi> newRsis = new ArrayList<>();
    double avgGain = 0;
    double avgLoss = 0;

    // Calculate initial avgGain and avgLoss if we have enough previous prices
    if (previousPrices.size() >= length) {
      List<Double> gains = new ArrayList<>();
      List<Double> losses = new ArrayList<>();

      for (int i = 1; i < previousPrices.size(); i++) {
        double change = previousPrices.get(i).getClose() - previousPrices.get(i - 1).getClose();
        gains.add(Math.max(change, 0));
        losses.add(Math.max(-change, 0));
      }

      avgGain = gains.stream().mapToDouble(Double::doubleValue).average().orElse(0);
      avgLoss = losses.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    // Maintain running window for tilt calculation
    List<BaseRsi> tiltWindow = new ArrayList<>(previousRsis);

    // Calculate new RSIs
    BasePrice prevPrice =
        previousPrices.isEmpty() ? null : previousPrices.get(previousPrices.size() - 1);

    for (BasePrice currentPrice : pricesToProcess) {
      if (prevPrice != null) {
        double change = currentPrice.getClose() - prevPrice.getClose();
        double currentGain = Math.max(change, 0);
        double currentLoss = Math.max(-change, 0);

        avgGain = ((avgGain * (length - 1)) + currentGain) / length;
        avgLoss = ((avgLoss * (length - 1)) + currentLoss) / length;

        double rs = avgLoss == 0 ? Double.POSITIVE_INFINITY : avgGain / avgLoss;
        double rsiValue;
        if (avgGain == 0 && avgLoss == 0) {
          rsiValue = 50.0; // Neutral when no movement
        } else if (avgLoss == 0) {
          rsiValue = 100.0; // All gains
        } else if (avgGain == 0) {
          rsiValue = 0.0;  // All losses
        } else {
          rsiValue = 100.0 - (100.0 / (1.0 + rs));
        }

        BaseRsi rsi = createRsi(timeFrame);
        rsi.setId(new RsiKey(ticker, currentPrice.getId().getDate(), length));
        rsi.setValue(rsiValue);

        // Update tilt calculation
        tiltWindow.add(rsi);
        if (tiltWindow.size() > TILT_PERIOD) {
          tiltWindow.remove(0);
        }
        if (tiltWindow.size() == TILT_PERIOD) {
          double tilt = Utils.calculateTilt(tiltWindow);
          rsi.setTilt(tilt);
        }

        newRsis.add(rsi);
      }
      prevPrice = currentPrice;
    }

    // Save new RSIs
    if (!newRsis.isEmpty()) {
      saveRsi(newRsis, timeFrame);
    }

    return newRsis;
  }

  private BaseRsi createRsi(TimeFrame timeFrame) {
    return switch (timeFrame) {
      case MIN5 -> new RsiMin5();
      case HOUR -> new RsiHour();
      case DAY -> new RsiDay();
      case WEEK -> new RsiWeek();
      case MONTH -> new RsiMonth();
    };
  }

  private BasePrice createBasePrice(TimeFrame timeFrame, PriceKey priceKey) {
    BasePrice price = switch (timeFrame) {
      case MIN5 -> new PriceMin5(priceKey, 0, 0, 0, 0, 0);
      case HOUR -> new PriceHour();
      case DAY -> new PriceDay();
      case WEEK -> new PriceWeek();
      case MONTH -> new PriceMonth();
    };
    if (timeFrame != TimeFrame.MIN5) {
      price.setId(priceKey);
    }
    return price;
  }

  private RsiRepository getRsiRepository(TimeFrame timeFrame) {
    return switch (timeFrame) {
      case MIN5 -> min5Repository;
      case HOUR -> hourRepository;
      case DAY -> dayRepository;
      case WEEK -> weekRepository;
      case MONTH -> monthRepository;
    };
  }

  private void saveRsi(List<? extends BaseRsi> rsiList, TimeFrame timeFrame) {
    // Filter out any RSIs with NaN values before saving
    List<? extends BaseRsi> validRsis = rsiList.stream()
        .filter(rsi -> !Double.isNaN(rsi.getValue()) && !Double.isNaN(rsi.getTilt()))
        .toList();

    switch (timeFrame) {
      case MIN5 -> min5Repository.saveAll((List<RsiMin5>) validRsis);
      case HOUR -> hourRepository.saveAll((List<RsiHour>) validRsis);
      case DAY -> dayRepository.saveAll((List<RsiDay>) validRsis);
      case WEEK -> weekRepository.saveAll((List<RsiWeek>) validRsis);
      case MONTH -> monthRepository.saveAll((List<RsiMonth>) validRsis);
    }
  }

  @Override
  public void calculateRsiForAllTimeFrames(String ticker, int length) {
    for (TimeFrame timeFrame : TimeFrame.values()) {
      if (timeFrame != TimeFrame.MIN5) {
        calculateRsi(ticker, length, timeFrame);
      }
    }
  }

  @Override
  public void calculateRsiForAllTimeFramesAndTickers(int length) {
    Set<String> tickers = priceDao.findUniqueTickers();
    for (String ticker : tickers) {
      calculateRsiForAllTimeFrames(ticker, length);
    }
  }

  @Override
  public void calculateIncrementalRsiForAllTickersAndTimeFrames(int length) {
    Set<String> tickers = priceDao.findUniqueTickers();
    for (String ticker : tickers) {
      calculateIncrementalRsiForAllTimeFrames(ticker, length);
    }
  }

  @Override
  public void calculateIncrementalRsiForAllTimeFrames(String ticker, int length) {
    System.out.println("RsiService ticker = " + ticker + " length = " + length);
    for (TimeFrame timeFrame : TimeFrame.values()) {
      if (timeFrame != TimeFrame.MIN5) {
        calculateIncrementalRsi(ticker, length, timeFrame);
      }
    }
  }
}