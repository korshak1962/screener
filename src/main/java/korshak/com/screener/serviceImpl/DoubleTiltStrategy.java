package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.service.SmaDao;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalTilt;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("DoubleTiltStrategy")
public class DoubleTiltStrategy implements Strategy {
  protected List<SignalTilt> signals;
  protected List<SignalTilt> signalsShort;
  protected List<SignalTilt> signalsLong;
  protected TimeFrame timeFrame = TimeFrame.DAY;  // Default value
  protected final SmaDao smaDao;
  protected final PriceDao priceDao;
  protected List<? extends BaseSma> smaShortList;
  protected List<Double> shortSmaTilt;
  protected List<? extends BaseSma> smaLongList;
  protected List<Double> trendSmaTilt;
  protected String ticker;
  protected List<? extends BasePrice> prices;
  protected double tiltLongOpen = 0.02;
  protected double tiltLongClose = -0.02;
  protected double tiltShortOpen = -0.02;
  protected double tiltShortClose = -0.02;
  protected double tiltHigherTrendLong = -10;
  protected double tiltHigherTrendShort = -20;
  protected int tiltPeriod = 5;
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
    double shortTilt = 0.0;
    double longTilt = 0.0;
    for (int i = 0; i < shortSmaTilt.size(); i++) {
      shortTilt = shortSmaTilt.get(i);
      longTilt = trendSmaTilt.get(i);
      if (shortTilt > tiltLongOpen && longTilt > tiltHigherTrendLong) {

        //close short if have
        if (!signalsShort.isEmpty() &&
            signalsShort.getLast().getSignalType() == SignalType.ShortOpen) {
          signalsShort.add(new SignalTilt(
              prices.get(i).getId().getDate(),
              prices.get(i).getClose(),
              SignalType.ShortClose, shortTilt, longTilt));
        }
        // then open long
        if (signalsLong.isEmpty() || signalsLong.getLast().getSignalType() != SignalType.LongOpen) {
          signalsLong.add(new SignalTilt(
              prices.get(i).getId().getDate(),
              prices.get(i).getClose(),
              SignalType.LongOpen, shortTilt, longTilt));
        }
      }
      if (shortTilt > tiltShortClose) {
        //close short
        if (!signalsShort.isEmpty() &&
            signalsShort.getLast().getSignalType() == SignalType.ShortOpen) {
          signalsShort.add(new SignalTilt(
              prices.get(i).getId().getDate(),
              prices.get(i).getClose(),
              SignalType.ShortClose, shortTilt, longTilt));
        }
      }
      if (shortTilt < tiltLongClose) {
        //close long
        if (!signalsLong.isEmpty() &&
            signalsLong.getLast().getSignalType() == SignalType.LongOpen) {
          signalsLong.add(new SignalTilt(
              prices.get(i).getId().getDate(),
              prices.get(i).getClose(),
              SignalType.LongClose, shortTilt, longTilt));
        }
      }
      if (shortTilt < tiltShortOpen && longTilt < tiltHigherTrendShort) {
        // open short
        if (signalsShort.isEmpty() ||
            signalsShort.getLast().getSignalType() != SignalType.ShortOpen) {
          signalsShort.add(new SignalTilt(
              prices.get(i).getId().getDate(),
              prices.get(i).getClose(),
              SignalType.ShortOpen, shortTilt, longTilt));
        }
      }
    }
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
    System.out.println(
        "======== shortTilt = " + shortTilt + " for date = " + prices.getLast().getId().getDate());
    System.out.println("======== longTilt = " + longTilt);
  }

  @Override
  public List<? extends Signal> getSignalsShort() {
    if (signalsShort == null) {
      calcSignals();
    }
    return signalsShort;
  }


  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.ticker = ticker;
    this.timeFrame = timeFrame;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public String getName() {
    return "DoubleTiltStrategy";
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

  public double getTiltLongOpen() {
    return tiltLongOpen;
  }

  public void setTiltLongOpen(double tiltLongOpen) {
    signalsShort = null;
    signalsLong = null;
    this.tiltLongOpen = tiltLongOpen;
  }

  public double getTiltLongClose() {
    return tiltLongClose;
  }

  public void setTiltLongClose(double tiltLongClose) {
    signalsShort = null;
    signalsLong = null;
    this.tiltLongClose = tiltLongClose;
  }

  public double getTiltHigherTrendLong() {
    return tiltHigherTrendLong;
  }

  public void setTiltHigherTrendLong(double tiltHigherTrendLong) {
    signalsShort = null;
    signalsLong = null;
    this.tiltHigherTrendLong = tiltHigherTrendLong;
  }

  public double getTiltHigherTrendShort() {
    return tiltHigherTrendShort;
  }

  public void setTiltHigherTrendShort(double tiltHigherTrendShort) {
    signalsShort = null;
    signalsLong = null;
    this.tiltHigherTrendShort = tiltHigherTrendShort;
  }

  public int getTiltPeriod() {
    return tiltPeriod;
  }

  public void setTiltPeriod(int tiltPeriod) {
    signalsShort = null;
    signalsLong = null;
    this.tiltPeriod = tiltPeriod;
  }

  public int getLongLength() {
    return longLength;
  }

  public void setTrendLengthSma(int longLength) {
    signalsShort = null;
    signalsLong = null;
    this.longLength = longLength;
    this.smaLongList = getSma(longLength);
    if (prices == null) {
      this.prices = priceDao.findByDateRange(
          ticker,
          smaLongList.get(tiltPeriod - 1).getId().getDate(),
          endDate,
          timeFrame
      );
    }
    this.trendSmaTilt = calculateTiltList(smaLongList);

  }

  public void setSmaLength(int shortLength) {
    signalsShort = null;
    signalsLong = null;
    this.shortLength = shortLength;
    this.smaShortList = getSma(shortLength);
    if (prices == null) {
      this.prices = priceDao.findByDateRange(
          ticker,
          smaShortList.get(tiltPeriod - 1).getId().getDate(),
          endDate,
          timeFrame
      );
    }
    if (prices == null || prices.isEmpty()) {
      throw new RuntimeException("prices.isEmpty()");
    }
    if (prices.size() != smaShortList.size() - tiltPeriod + 1) {
      throw new RuntimeException("prices.size() != smaShortList.size()-tiltPeriod");
    }
    this.shortSmaTilt = calculateTiltList(smaShortList);
  }

  public List<? extends BaseSma> getSma(int length) {
    return smaDao.findByDateRange(
        ticker,
        startDate,
        endDate,
        timeFrame,
        length
    );
  }

  private List<Double> calculateTiltList(List<? extends BaseSma> smaList) {
    List<Double> tilts = new ArrayList<>();
    boolean checkStart = true;
    // Create a sliding window for tilt calculation
    List<BaseSma> slidingWindow = new ArrayList<>();
    for (BaseSma currentSma : smaList) {
      // Maintain sliding window
      slidingWindow.add(currentSma);
      if (slidingWindow.size() > tiltPeriod) {
        slidingWindow.remove(0);
      }
      // Only calculate tilt when we have enough data points

      if (slidingWindow.size() == tiltPeriod) {
        if (checkStart) {
          checkStart = false;
          if (!prices.getFirst().getId().getDate()
              .equals(slidingWindow.getLast().getId().getDate())) {
            throw new RuntimeException("Prices and tilts are NOT synchronized!");
          }
        }
        double currentTilt = calculateTilt(slidingWindow);
        tilts.add(currentTilt);
      }
    }
    return tilts;
  }

  public List<? extends BaseSma> getSmaShortList() {
    return smaShortList;
  }

  public List<? extends BaseSma> getSmaLongList() {
    return smaLongList;
  }

  public List<Double> getShortSmaTilt() {
    return shortSmaTilt;
  }

  public List<Double> getTrendSmaTilt() {
    return trendSmaTilt;
  }

  public NavigableMap<LocalDateTime, Double> getShortSmaTiltAsMap() {
    NavigableMap<LocalDateTime, Double> dateToTilt = new TreeMap<>();
    for (int i = 0; i < shortSmaTilt.size(); i++) {
      dateToTilt.put(prices.get(i).getId().getDate(), shortSmaTilt.get(i));
    }
    return dateToTilt;
  }

  public NavigableMap<LocalDateTime, Double> getTrendSmaTiltAsMap() {
    NavigableMap<LocalDateTime, Double> dateToTilt = new TreeMap<>();
    for (int i = 0; i < trendSmaTilt.size(); i++) {
      dateToTilt.put(prices.get(i).getId().getDate(), trendSmaTilt.get(i));
    }
    return dateToTilt;
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

  public List<? extends BasePrice> getPrices() {
    return prices;
  }
}
