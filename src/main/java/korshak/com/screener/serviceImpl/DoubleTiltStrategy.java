package korshak.com.screener.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.PriceDao;
import korshak.com.screener.service.SmaDao;
import korshak.com.screener.service.Strategy;
import korshak.com.screener.vo.SignalTilt;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("DoubleTiltStrategy")
public class DoubleTiltStrategy implements Strategy {
  private List<SignalTilt> signals;
  private TimeFrame timeFrame = TimeFrame.DAY;  // Default value
  private final SmaDao smaDao;
  private final PriceDao priceDao;
  private List<? extends BaseSma> smaShortList;
  private List<Double> shortSmaTilt;
  private List<? extends BaseSma> smaLongList;
  private List<Double> longSmaTilt;
  private String ticker;
  private List<? extends BasePrice> prices;
  private double tiltShortBuy = 0.02;
  private double tiltShortSell = -0.02;
  private double tiltLongBuy = -100;
  private double tiltLongSell = -200;
  private int tiltPeriod = 5;
  private int longLength;
  private int shortLength;
  private LocalDateTime startDate;
  private LocalDateTime endDate;

  public DoubleTiltStrategy(SmaDao smaDao, PriceDao priceDao) {
    this.smaDao = smaDao;
    this.priceDao = priceDao;
  }


  @Override
  public List<SignalTilt> getSignals(List<? extends BasePrice> prices) {

    return getSignals();
  }

  @Override
  public List<SignalTilt> getSignals() {
    signals = new ArrayList<>();
    double shortTilt = 0.0;
    double longTilt = 0.0;
    for (int i = 0; i < shortSmaTilt.size(); i++) {
      shortTilt = shortSmaTilt.get(i);
      longTilt = shortSmaTilt.get(i);
      if ((signals.isEmpty() || signals.getLast().getAction() == SignalType.Sell)
          && shortTilt > tiltShortBuy && longTilt > tiltLongBuy) { // open long
        SignalTilt signalTilt = new SignalTilt(
            prices.get(i).getId().getDate(),
            prices.get(i).getClose(),
            SignalType.Buy
        );
        signals.add(signalTilt);
        signalTilt.setShortTilt(shortTilt);
      } else if (signals.isEmpty() || signals.getLast().getAction() == SignalType.Buy
          && shortTilt < tiltShortSell) { //  close long
        SignalTilt signalTilt = new SignalTilt(
            prices.get(i).getId().getDate(),
            prices.get(i).getClose(),
            SignalType.Sell
        );
        signals.add(signalTilt);
        signalTilt.setShortTilt(shortTilt);
      }
    }
    SignalTilt last = signals.getLast();
    if (last.getAction() == SignalType.Buy) {
      System.out.println("======== Last Signal " + last);
    }
    System.out.println("======== shortTilt = " + shortTilt);
    System.out.println("======== longTilt = " + longTilt);
    return signals;
  }


  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.ticker = ticker;
    this.timeFrame = timeFrame;
    this.startDate = startDate;
    this.endDate = endDate;
    this.prices = priceDao.findByDateRange(
        ticker,
        startDate,
        endDate,
        timeFrame
    );
    if (prices == null || prices.isEmpty()) {
      throw new RuntimeException("prices.isEmpty()");
    }
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

  public double getTiltShortBuy() {
    return tiltShortBuy;
  }

  public void setTiltShortBuy(double tiltShortBuy) {
    this.tiltShortBuy = tiltShortBuy;
  }

  public double getTiltShortSell() {
    return tiltShortSell;
  }

  public void setTiltShortSell(double tiltShortSell) {
    this.tiltShortSell = tiltShortSell;
  }

  public double getTiltLongBuy() {
    return tiltLongBuy;
  }

  public void setTiltLongBuy(double tiltLongBuy) {
    this.tiltLongBuy = tiltLongBuy;
  }

  public double getTiltLongSell() {
    return tiltLongSell;
  }

  public void setTiltLongSell(double tiltLongSell) {
    this.tiltLongSell = tiltLongSell;
  }

  public int getTiltPeriod() {
    return tiltPeriod;
  }

  public void setTiltPeriod(int tiltPeriod) {
    this.tiltPeriod = tiltPeriod;
  }

  public int getLongLength() {
    return longLength;
  }

  public void setLongLength(int longLength) {
    this.longLength = longLength;
    this.smaLongList = getSma(longLength);
    if (prices.size() != smaLongList.size()) {
      throw new RuntimeException("prices.size()!=smaLongList.size()");
    }
    this.longSmaTilt = calculateTiltList(smaLongList);
  }

  public void setShortLength(int shortLength) {
    this.shortLength = shortLength;
    this.smaShortList = getSma(shortLength);
    if (prices.size() != smaShortList.size()) {
      throw new RuntimeException("prices.size()!=smaShortList.size()");
    }
    if (!prices.getFirst().getId().getDate().equals(smaShortList.getFirst().getId().getDate())) {
      throw new RuntimeException(
          "prices.getFirst().getId().getDate()!=smaShortList.getFirst().getId().getDate()");
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

  public List<Double> getLongSmaTilt() {
    return longSmaTilt;
  }

  public NavigableMap<LocalDateTime, Double> getshortSmaTilt(){
    NavigableMap<LocalDateTime, Double> dateToTilt = new TreeMap<>();
    for (int i=0;i<shortSmaTilt.size();i++){
      dateToTilt.put(prices.get(i).getId().getDate(),shortSmaTilt.get(i));
    }
    return dateToTilt;
  }
}
