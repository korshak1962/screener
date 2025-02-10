package korshak.com.screener.utils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.SmaDay;
import korshak.com.screener.dao.SmaHour;
import korshak.com.screener.dao.SmaMonth;
import korshak.com.screener.dao.SmaWeek;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;

public class Utils {
  public static NavigableMap<LocalDateTime, Double> convertBaseSmaListToTreeMap(List<? extends BaseSma> sortedSmaList){
    NavigableMap<LocalDateTime, Double> map = new TreeMap<>();
    for (BaseSma sma : sortedSmaList) {
      map.put(sma.getId().getDate(), sma.getValue());
    }
    return map;
  }

  public static double calculateYield(TimeFrame timeFrame, double baseTilt) {
    // Convert to annual percentage based on timeframe
    return baseTilt * switch(timeFrame) {
      case MIN5 -> 365.0 * 24 * 12;  // minutes per year
      case HOUR -> 365.0 * 24;       // hours per year
      case DAY -> 365.0;             // days per year
      case WEEK -> 52.0;             // weeks per year
      case MONTH -> 12.0;            // months per year
    };
  }

  public static double calculateTilt(List<BaseSma> smaWindow) {
    // Calculate linear regression slope as tilt
    int n = smaWindow.size();
    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumXX = 0;

    for (int i = 0; i < n; i++) {
      double x = i;
      double y = smaWindow.get(i).getValue();
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

  public static BaseSma getBaseSma(TimeFrame timeFrame) {
    return switch (timeFrame) {
      case HOUR -> new SmaHour();
      case DAY -> new SmaDay();
      case WEEK -> new SmaWeek();
      case MONTH -> new SmaMonth();
      default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeFrame);
    };
  }

  public static Signal fillLongShortLists(Signal signal, Signal lastSignal, List<Signal> signalsShort,
                                          List<Signal> signalsLong) {
    switch (signal.getSignalType()) {
      case SignalType.LongOpen:
        if (lastSignal != null && lastSignal.getSignalType() == SignalType.LongOpen) {
          return lastSignal;
        }
        if (lastSignal != null && lastSignal.getSignalType() == SignalType.ShortOpen) {
          signalsShort.add(createSignal(signal, SignalType.ShortClose));
        }
        lastSignal = createSignal(signal, SignalType.LongOpen);
        signalsLong.add(lastSignal);
        break;
      case SignalType.ShortClose:
        if (lastSignal == null) {
          return lastSignal;
        }
        if (lastSignal.getSignalType() == SignalType.ShortOpen) {
          lastSignal = createSignal(signal, SignalType.ShortClose);
          signalsShort.add(lastSignal);
        }
        break;
      case SignalType.LongClose:
        if (lastSignal == null) {
          return lastSignal;
        }
        if (lastSignal.getSignalType() == SignalType.LongOpen) {
          lastSignal = createSignal(signal, SignalType.LongClose);
          signalsLong.add(lastSignal);
        }
        break;
      case SignalType.ShortOpen:
        if (lastSignal != null && lastSignal.getSignalType() == SignalType.ShortOpen) {
          return lastSignal;
        }
        if (lastSignal != null && lastSignal.getSignalType() == SignalType.LongOpen) {
          signalsLong.add(createSignal(signal, SignalType.LongClose));
        }
        lastSignal = createSignal(signal, SignalType.ShortOpen);
        signalsShort.add(lastSignal);
        break;
      default:
        break;
    }
    return lastSignal;
  }

  public static Signal createSignal(Signal signal, SignalType longOpen) {
    return new Signal(
        signal.getDate(),
        signal.getPrice(),
        longOpen);
  }

  public static Signal createSignal(BasePrice price, SignalType longOpen) {
    return new Signal(
        price.getId().getDate(),
        price.getClose(),
        longOpen);
  }

  public static Signal createSignal(BasePrice price, SignalType longOpen, double precizePrice) {
    return new Signal(
        price.getId().getDate(),
        precizePrice,
        longOpen);
  }
}
