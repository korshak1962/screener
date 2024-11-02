package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.SmaDao;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.vo.Trade;
import org.springframework.stereotype.Service;

@Service("TiltStrategy")
public class TiltStrategy implements Strategy {
  private double tiltBuy = 0.01;        // Default value
  private double tiltSell = -0.01;      // Default value
  private int tiltPeriod = 5;          // Default value
  private int length = 9;             // Default value
  private TimeFrame timeFrame = TimeFrame.DAY;  // Default value
  private final SmaDao smaDao;

  public TiltStrategy(SmaDao smaDao) {
    this.smaDao = smaDao;
  }

  // Getters and Setters for all parameters
  public double getTiltBuy() {
    return tiltBuy;
  }

  public void setTiltBuy(double tiltBuy) {
    if (tiltBuy <= tiltSell) {
      throw new IllegalArgumentException("Tilt buy threshold must be greater than tilt sell threshold");
    }
    this.tiltBuy = tiltBuy;
  }

  public double getTiltSell() {
    return tiltSell;
  }

  public void setTiltSell(double tiltSell) {
    if (tiltSell >= tiltBuy) {
      throw new IllegalArgumentException("Tilt sell threshold must be less than tilt buy threshold");
    }
    this.tiltSell = tiltSell;
  }

  public int getTiltPeriod() {
    return tiltPeriod;
  }

  public void setTiltPeriod(int tiltPeriod) {
    if (tiltPeriod <= 0) {
      throw new IllegalArgumentException("Tilt period must be positive");
    }
    this.tiltPeriod = tiltPeriod;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("SMA length must be positive");
    }
    this.length = length;
  }

  public TimeFrame getTimeFrame() {
    return timeFrame;
  }

  public void setTimeFrame(TimeFrame timeFrame) {
    if (timeFrame == null) {
      throw new IllegalArgumentException("TimeFrame cannot be null");
    }
    this.timeFrame = timeFrame;
  }

  /**
   * Sets both buy and sell thresholds at once, ensuring they maintain proper relationship
   * @param tiltBuy the buy threshold
   * @param tiltSell the sell threshold
   * @throws IllegalArgumentException if tiltBuy <= tiltSell
   */
  public void setTiltThresholds(double tiltBuy, double tiltSell) {
    if (tiltBuy <= tiltSell) {
      throw new IllegalArgumentException("Tilt buy threshold must be greater than tilt sell threshold");
    }
    this.tiltBuy = tiltBuy;
    this.tiltSell = tiltSell;
  }

  /**
   * Resets all parameters to their default values
   */
  public void resetToDefaults() {
    this.tiltBuy = 0.5;
    this.tiltSell = -0.3;
    this.tiltPeriod = 5;
    this.length = 20;
    this.timeFrame = TimeFrame.DAY;
  }

  @Override
  public List<Trade> getTrades(List<? extends BasePrice> prices) {
    List<Trade> trades = new ArrayList<>();
    if (prices == null || prices.isEmpty()) {
      return trades;
    }

    String ticker = prices.get(0).getId().getTicker();
    LocalDateTime startDate = prices.get(0).getId().getDate();
    LocalDateTime endDate = prices.get(prices.size() - 1).getId().getDate();

    List<? extends BaseSma> smaList = smaDao.findByDateRange(
        ticker,
        startDate,
        endDate,
        timeFrame,
        length
    );

    if (smaList.isEmpty()) throw new RuntimeException("No SMAs found");

    Map<LocalDateTime, BaseSma> smaMap = smaList.stream()
        .collect(Collectors.toMap(
            sma -> sma.getId().getDate(),
            sma -> sma
        ));

    double previousTilt = 0;
    boolean inPosition = false;

    // Need at least tiltPeriod + 1 SMAs to calculate tilt
    if (smaList.size() <= tiltPeriod) {
      return trades;
    }

    // Create a sliding window for tilt calculation
    List<BaseSma> slidingWindow = new ArrayList<>();

    for (BasePrice price : prices) {
      LocalDateTime currentDate = price.getId().getDate();
      BaseSma currentSma = smaMap.get(currentDate);

      if (currentSma == null) {
        continue;
      }

      // Maintain sliding window
      slidingWindow.add(currentSma);
      if (slidingWindow.size() > tiltPeriod) {
        slidingWindow.remove(0);
      }

      // Only calculate tilt when we have enough data points
      if (slidingWindow.size() == tiltPeriod) {
        double currentTilt = calculateTilt(slidingWindow);

        if (!inPosition && currentTilt > tiltBuy && previousTilt <= tiltBuy) {
          trades.add(new Trade(
              currentDate,
              price.getClose(),
              1,  // buy
              1   // 1 share
          ));
          inPosition = true;
        }
        else if (inPosition && currentTilt < tiltSell && previousTilt >= tiltSell) {
          trades.add(new Trade(
              currentDate,
              price.getClose(),
              -1, // sell
              1   // 1 share
          ));
          inPosition = false;
        }

        previousTilt = currentTilt;
      }
    }

    return trades;
  }

  @Override
  public String getName() {
    return "TiltSmaStrategy";
  }

  private double calculateTilt(List<BaseSma> smaWindow) {
    // Calculate linear regression slope as tilt
    int n = smaWindow.size();
    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumXX = 0;

    for (int i = 0; i < n; i++) {
      double x = i;
      double y = smaWindow.get(i).getValue(); // Replace getValue() with actual method if different

      sumX += x;
      sumY += y;
      sumXY += x * y;
      sumXX += x * x;
    }

    // Calculate slope using least squares method
    double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);

    // Normalize the slope by the average SMA value to get a percentage
    double avgSma = sumY / n;
    double normalizedSlope = (slope / avgSma) * 100;

    return normalizedSlope;
  }
}
