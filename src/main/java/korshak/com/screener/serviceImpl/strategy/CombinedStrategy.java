package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

@Service("CombinedStrategy")
public class CombinedStrategy implements Strategy {
  double tiltBuy = 1;
  double tiltSell = -1;
  int length;
  TimeFrame timeFrame;
  LocalDateTime startDate;
  LocalDateTime endDate;
  final SmaDao smaDao;
  final PriceDao priceDao;
  List<? extends BaseSma> smaList;
  String ticker;
  List<? extends BasePrice> prices;
  TreeMap<LocalDateTime, Double> dateToTiltValue = new TreeMap<>();
  List<Signal> signalsLong = new ArrayList<>();
  List<Signal> signalsShort = new ArrayList<>();

  public CombinedStrategy(SmaDao smaDao, PriceDao priceDao) {
    this.smaDao = smaDao;
    this.priceDao = priceDao;
  }

  public void calcSignals() {
    if (prices == null || prices.isEmpty()) {
      throw new RuntimeException("Prices are not initialized");
    }
    Map<LocalDateTime, BaseSma> smaMap = smaList.stream()
        .collect(Collectors.toMap(
            sma -> sma.getId().getDate(),
            sma -> sma
        ));
    // Iterate through prices and check stored tilt values
    Signal lastSignal = null;
    for (BasePrice price : prices) {
      List<Signal> signalsForPrice = new ArrayList<>();
      BaseSma currentSma = smaMap.get(price.getId().getDate());
      Signal signalToAdd = getSignal(price, currentSma);
      if (signalToAdd != null) {
        signalsForPrice.add(signalToAdd);
      }
      //  decision make
      if (signalsForPrice.isEmpty()) {
        continue;
      }
      Signal signalMin = Collections.min(signalsForPrice,
          Comparator.comparingInt(sgnal -> sgnal.getSignalType().value));

      lastSignal = Utils.fillLongShortLists(signalMin, lastSignal, signalsShort, signalsLong);
    }
  }

  private Signal getSignal(BasePrice price, BaseSma currentSma) {
    Signal signalToAdd = null;
    if (currentSma == null) {
      return signalToAdd;
    }
    if (currentSma.getTilt() > tiltBuy) {
      signalToAdd = Utils.createSignal(price, SignalType.LongOpen);
    } else if (currentSma.getTilt() < tiltSell) {
      signalToAdd = Utils.createSignal(price, SignalType.LongClose);
    }
    return signalToAdd;
  }

  @Override
  public List<? extends Signal> getSignalsLong() {
    return signalsLong;
  }

  @Override
  public List<? extends Signal> getSignalsShort() {
    return signalsShort;
  }

  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {
    this.timeFrame = timeFrame;
    this.ticker = ticker;
    this.startDate = startDate;
    this.endDate = endDate;
    this.prices = priceDao.findByDateRange(
        ticker,
        startDate,
        endDate,
        timeFrame
    );
  }

  @Override
  public String getName() {
    return "TiltCombinedStrategy";
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
    this.smaList = smaDao.findByDateRangeOrderByIdDateAsc(
        ticker,
        startDate,
        endDate,
        timeFrame,
        length
    );
    if (smaList.isEmpty() || prices.size() - smaList.size() > length) {
      throw new RuntimeException("No SMAs found");
    }
  }

  public TimeFrame getTimeFrame() {
    return timeFrame;
  }

  @Override
  public String getTicker() {
    return ticker;
  }

  @Override
  public LocalDateTime getStartDate() {
    return startDate;
  }

  @Override
  public LocalDateTime getEndDate() {
    return endDate;
  }

  public List<? extends BaseSma> getSmaList() {
    return smaList;
  }

  public TreeMap<LocalDateTime, Double> getDateToTiltValue() {
    return dateToTiltValue;
  }

}
