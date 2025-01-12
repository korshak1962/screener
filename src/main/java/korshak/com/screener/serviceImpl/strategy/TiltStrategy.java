package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.SmaDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("TiltStrategy")
public class TiltStrategy implements Strategy {
  private double tiltBuy;
  private double tiltSell;
  private int length;
  private TimeFrame timeFrame;
  private final SmaDao smaDao;
  private final PriceDao priceDao;
  private List<? extends BaseSma> smaList;
  private String ticker;
  private List<? extends BasePrice> prices;
  private TreeMap<LocalDateTime, Double> dateToTiltValue = new TreeMap<>();

  public TiltStrategy(SmaDao smaDao, PriceDao priceDao) {
    this.smaDao = smaDao;
    this.priceDao = priceDao;
  }

  @Override
  public List<Signal> getSignalsLong() {
    List<Signal> signals = new ArrayList<>();
    if (prices == null || prices.isEmpty()) {
      return signals;
    }

    ticker = prices.get(0).getId().getTicker();

    if (smaList.isEmpty() || prices.size() - smaList.size() > length) {
      throw new RuntimeException("No SMAs found");
    }

    Map<LocalDateTime, BaseSma> smaMap = smaList.stream()
        .collect(Collectors.toMap(
            sma -> sma.getId().getDate(),
            sma -> sma
        ));

    boolean inPosition = false;
    double previousTilt = 0;

    // Iterate through prices and check stored tilt values
    for (BasePrice price : prices) {
      LocalDateTime currentDate = price.getId().getDate();
      BaseSma currentSma = smaMap.get(currentDate);

      if (currentSma == null) {
        continue;
      }

      double currentTilt = currentSma.getTilt();
      dateToTiltValue.put(currentDate, currentTilt);

      // Generate signals based on tilt thresholds
      if (!inPosition && currentTilt > tiltBuy && previousTilt <= tiltBuy) {
        signals.add(new Signal(
            currentDate,
            price.getClose(),
            SignalType.LongOpen
        ));
        inPosition = true;
      } else if (inPosition && currentTilt < tiltSell && previousTilt >= tiltSell) {
        signals.add(new Signal(
            currentDate,
            price.getClose(),
            SignalType.LongClose
        ));
        inPosition = false;
      }

      previousTilt = currentTilt;
    }

    return signals;
  }

  @Override
  public List<? extends Signal> getSignalsShort() {
    return List.of();
  }

  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.timeFrame = timeFrame;
    length = 9;
    tiltBuy = 0.02;
    tiltSell = -0.02;
    this.prices = priceDao.findByDateRange(
        ticker,
        startDate,
        endDate,
        timeFrame
    );
    this.smaList = smaDao.findByDateRangeOrderByIdDateAsc(
        ticker,
        startDate,
        endDate,
        timeFrame,
        length
    );
    ;
  }

  @Override
  public String getName() {
    return "TiltSmaStrategy";
  }

  @Override
  public List<? extends BasePrice> getPrices() {
    return prices;
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getIndicators() {
    Map<String, NavigableMap<LocalDateTime, Double>> indicators = new HashMap<>();
    indicators.put("Tilt", getDateToTiltValue());
    return indicators;
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators() {
    Map<String, NavigableMap<LocalDateTime, Double>> priceIndicators = new HashMap<>();
    priceIndicators.put("SMA_" + smaList.getFirst().getId().getLength(),
        Utils.convertBaseSmaListToTreeMap(smaList));
    return priceIndicators;
  }

  // Getters and setters
  public double getTiltBuy() {
    return tiltBuy;
  }

  public void setTiltBuy(double tiltBuy) {
    if (tiltBuy <= tiltSell) {
      throw new IllegalArgumentException(
          "Tilt buy threshold must be greater than tilt sell threshold");
    }
    this.tiltBuy = tiltBuy;
  }

  public double getTiltSell() {
    return tiltSell;
  }

  public void setTiltSell(double tiltSell) {
    if (tiltSell >= tiltBuy) {
      throw new IllegalArgumentException(
          "Tilt sell threshold must be less than tilt buy threshold");
    }
    this.tiltSell = tiltSell;
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

  public void setTiltThresholds(double tiltBuy, double tiltSell) {
    if (tiltBuy <= tiltSell) {
      throw new IllegalArgumentException(
          "Tilt buy threshold must be greater than tilt sell threshold");
    }
    this.tiltBuy = tiltBuy;
    this.tiltSell = tiltSell;
  }

  public List<? extends BaseSma> getSmaList() {
    return smaList;
  }

  public TreeMap<LocalDateTime, Double> getDateToTiltValue() {
    return dateToTiltValue;
  }

  public void resetToDefaults() {
    this.tiltBuy = 0.5;
    this.tiltSell = -0.3;
    this.length = 20;
    this.timeFrame = TimeFrame.DAY;
  }
}