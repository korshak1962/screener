package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.SmaDao;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalTilt;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("DoubleTiltStrategy")
public class DoubleTiltStrategy implements Strategy {
  protected List<SignalTilt> signals;
  protected List<SignalTilt> signalsShort;
  protected List<SignalTilt> signalsLong;
  protected TimeFrame timeFrame = TimeFrame.DAY;
  protected final SmaDao smaDao;
  protected final PriceDao priceDao;
  protected List<? extends BaseSma> smaShortList;
  protected List<? extends BaseSma> smaLongList;
  protected String ticker;
  protected List<? extends BasePrice> prices;
  protected double tiltLongOpen = 0.02;
  protected double tiltLongClose = -0.02;
  protected double tiltShortOpen = -0.02;
  protected double tiltShortClose = -0.02;
  protected double tiltHigherTrendLong = -10;
  protected double tiltHigherTrendShort = -20;
  protected int longLength;
  protected int shortLength;
  protected LocalDateTime startDate;
  protected LocalDateTime endDate;

  public DoubleTiltStrategy(SmaDao smaDao, PriceDao priceDao) {
    this.smaDao = smaDao;
    this.priceDao = priceDao;
  }

  @Override
  public List<SignalTilt> getSignalsLong() {
    if (signalsLong == null) {
      calcSignals();
    }
    return signalsLong;
  }

  public void calcSignals() {
    signalsLong = new ArrayList<>();
    signalsShort = new ArrayList<>();

    // Create maps for easier access to SMAs by date
    Map<LocalDateTime, BaseSma> shortSmaMap = new LinkedHashMap<>();
    Map<LocalDateTime, BaseSma> longSmaMap = new LinkedHashMap<>();

    for (BaseSma sma : smaShortList) {
      shortSmaMap.put(sma.getId().getDate(), sma);
    }
    for (BaseSma sma : smaLongList) {
      longSmaMap.put(sma.getId().getDate(), sma);
    }

    // Process signals based on stored tilt values
    for (BasePrice price : prices) {
      LocalDateTime currentDate = price.getId().getDate();
      BaseSma shortSma = shortSmaMap.get(currentDate);
      BaseSma longSma = longSmaMap.get(currentDate);

      if (shortSma == null || longSma == null) {
        continue;
      }

      double shortTilt = shortSma.getTilt();
      double longTilt = longSma.getTilt();

      makeDecision(price, shortTilt, longTilt, currentDate);
    }
    logLastPositions();
  }

  private void makeDecision(BasePrice price, double shortTilt, double longTilt,
                         LocalDateTime currentDate) {
    // Long position logic
    if (shortTilt > tiltLongOpen && longTilt > tiltHigherTrendLong) {
      closeShortOpenLong(price, currentDate, shortTilt, longTilt);
    }
    // Short position closure on positive short tilt
    if (shortTilt > tiltShortClose) {
      closeShort(price, currentDate, shortTilt, longTilt);
    }
    // Long position closure on negative short tilt
    if (shortTilt < tiltLongClose) {
      closeLong(price, currentDate, shortTilt, longTilt);
    }
    // Short position opening
    if (shortTilt < tiltShortOpen && longTilt < tiltHigherTrendShort) {
      openShort(price, currentDate, shortTilt, longTilt);
    }
  }

  private void openShort(BasePrice price, LocalDateTime currentDate, double shortTilt,
                         double longTilt) {
    if (signalsShort.isEmpty() ||
        signalsShort.getLast().getSignalType() != SignalType.ShortOpen) {
      signalsShort.add(new SignalTilt(
          currentDate,
          price.getClose(),
          SignalType.ShortOpen,
          shortTilt,
          longTilt
      ));
    }
  }

  private void closeLong(BasePrice price, LocalDateTime currentDate, double shortTilt,
                         double longTilt) {
    if (!signalsLong.isEmpty() &&
        signalsLong.getLast().getSignalType() == SignalType.LongOpen) {
      signalsLong.add(new SignalTilt(
          currentDate,
          price.getClose(),
          SignalType.LongClose,
          shortTilt,
          longTilt
      ));
    }
  }

  private void closeShort(BasePrice price, LocalDateTime currentDate, double shortTilt,
                         double longTilt) {
    if (!signalsShort.isEmpty() &&
        signalsShort.getLast().getSignalType() == SignalType.ShortOpen) {
      signalsShort.add(new SignalTilt(
          currentDate,
          price.getClose(),
          SignalType.ShortClose,
          shortTilt,
          longTilt
      ));
    }
  }

  private void closeShortOpenLong(BasePrice price, LocalDateTime currentDate, double shortTilt,
                                  double longTilt) {
    // Close short if open
    closeShort(price, currentDate, shortTilt, longTilt);
    // Open long if not already open
    if (signalsLong.isEmpty() ||
        signalsLong.getLast().getSignalType() != SignalType.LongOpen) {
      signalsLong.add(new SignalTilt(
          currentDate,
          price.getClose(),
          SignalType.LongOpen,
          shortTilt,
          longTilt
      ));
    }
  }

  private void logLastPositions() {
    if (!signalsLong.isEmpty()) {
      SignalTilt lastLong = signalsLong.getLast();
      if (lastLong.getSignalType() == SignalType.LongOpen) {
        System.out.println("======== lastLong Signal " + lastLong);
      }
    }
    if (!signalsShort.isEmpty()) {
      SignalTilt lastShort = signalsShort.getLast();
      if (lastShort.getSignalType() == SignalType.ShortOpen) {
        System.out.println("======== lastShort Signal " + lastShort);
      }
    }
  }

  @Override
  public List<? extends Signal> getSignalsShort() {
    if (signalsShort == null) {
      calcSignals();
    }
    return signalsShort;
  }

  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate, LocalDateTime endDate) {
    this.ticker = ticker;
    this.timeFrame = timeFrame;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public String StrategyName() {
    return "DoubleTiltStrategy";
  }

  public List<? extends BaseSma> getSmaOrderByIdDateAsc(int length) {
    return smaDao.findByDateRangeOrderByIdDateAsc(
        ticker,
        startDate,
        endDate,
        timeFrame,
        length
    );
  }

  public void setSmaLength(int shortLength) {
    signalsShort = null;
    signalsLong = null;
    this.shortLength = shortLength;
    this.smaShortList = getSmaOrderByIdDateAsc(shortLength);
    if (prices == null) {
      this.prices = priceDao.findByDateRange(
          ticker,
          startDate,
          endDate,
          timeFrame
      );
    }
  }

  public void setTrendLengthSma(int longLength) {
    signalsShort = null;
    signalsLong = null;
    this.longLength = longLength;
    this.smaLongList = getSmaOrderByIdDateAsc(longLength);
    if (prices == null) {
      this.prices = priceDao.findByDateRange(
          ticker,
          startDate,
          endDate,
          timeFrame
      );
    }
  }

  // Getters and setters for tilt thresholds
  // ... (keep existing getters and setters)

  public NavigableMap<LocalDateTime, Double> getShortSmaTiltAsMap() {
    NavigableMap<LocalDateTime, Double> dateToTilt = new TreeMap<>();
    for (BaseSma sma : smaShortList) {
      dateToTilt.put(sma.getId().getDate(), sma.getTilt());
    }
    return dateToTilt;
  }

  public NavigableMap<LocalDateTime, Double> getTrendSmaTiltAsMap() {
    NavigableMap<LocalDateTime, Double> dateToTilt = new TreeMap<>();
    for (BaseSma sma : smaLongList) {
      dateToTilt.put(sma.getId().getDate(), sma.getTilt());
    }
    return dateToTilt;
  }

  @Override
  public List<? extends BasePrice> getPrices() {
    return prices;
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getIndicators() {
    return Map.of();
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators() {
    return Map.of();
  }

  public List<? extends BaseSma> getSmaShortList() {
    return smaShortList;
  }

  public List<? extends BaseSma> getSmaLongList() {
    return smaLongList;
  }

  public double getTiltLongOpen() {
    return tiltLongOpen;
  }

  public void setTiltLongOpen(double tiltLongOpen) {
    this.tiltLongOpen = tiltLongOpen;
  }

  public double getTiltLongClose() {
    return tiltLongClose;
  }

  public void setTiltLongClose(double tiltLongClose) {
    this.tiltLongClose = tiltLongClose;
  }

  public double getTiltShortOpen() {
    return tiltShortOpen;
  }

  public void setTiltShortOpen(double tiltShortOpen) {
    this.tiltShortOpen = tiltShortOpen;
  }

  public double getTiltShortClose() {
    return tiltShortClose;
  }

  public void setTiltShortClose(double tiltShortClose) {
    this.tiltShortClose = tiltShortClose;
  }

  public double getTiltHigherTrendLong() {
    return tiltHigherTrendLong;
  }

  public void setTiltHigherTrendLong(double tiltHigherTrendLong) {
    this.tiltHigherTrendLong = tiltHigherTrendLong;
  }

  public double getTiltHigherTrendShort() {
    return tiltHigherTrendShort;
  }

  public void setTiltHigherTrendShort(double tiltHigherTrendShort) {
    this.tiltHigherTrendShort = tiltHigherTrendShort;
  }

  @Override
  public TimeFrame getTimeFrame() {
    return timeFrame;
  }

  public String getTicker() {
    return ticker;
  }

  public LocalDateTime getStartDate() {
    return startDate;
  }

  public LocalDateTime getEndDate() {
    return endDate;
  }

  @Override
  public List<Signal> getAllSignals() {
    return List.of();
  }

  @Override
  public List<Signal> getAllSignals(TimeFrame timeFrame) {
    return List.of();
  }
}
