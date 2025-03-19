package korshak.com.screener.utils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.OptParam;
import korshak.com.screener.dao.SmaDay;
import korshak.com.screener.dao.SmaHour;
import korshak.com.screener.dao.SmaMonth;
import korshak.com.screener.dao.SmaWeek;
import korshak.com.screener.dao.TiltableIndicator;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;

public class Utils {
  public static NavigableMap<LocalDateTime, Double> convertBaseSmaListToTreeMap(
      List<? extends BaseSma> sortedSmaList) {
    NavigableMap<LocalDateTime, Double> map = new TreeMap<>();
    for (BaseSma sma : sortedSmaList) {
      map.put(sma.getId().getDate(), sma.getValue());
    }
    return map;
  }

  public static double calculateYield(TimeFrame timeFrame, double baseTilt) {
    // Convert to annual percentage based on timeframe
    return baseTilt * switch (timeFrame) {
      case MIN5 -> 365.0 * 24 * 12;  // minutes per year
      case HOUR -> 365.0 * 24;       // hours per year
      case DAY -> 365.0;             // days per year
      case WEEK -> 52.0;             // weeks per year
      case MONTH -> 12.0;            // months per year
    };
  }

  public static double calculateTilt(List<? extends TiltableIndicator> window) {
    // Check if we have enough data points
    if (window == null || window.size() < 2) {
      return 0.0;
    }

    int n = window.size();
    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumXX = 0;

    // First pass: calculate sums and check for NaN values
    for (int i = 0; i < n; i++) {
      double value = window.get(i).getValue();
      if (Double.isNaN(value)) {
        return 0.0;  // Return 0 if we encounter any NaN values
      }
      sumX += i;
      sumY += value;
      sumXY += i * value;
      sumXX += i * i;
    }

    // Calculate denominator first to check for division by zero
    double denominator = n * sumXX - sumX * sumX;
    if (denominator == 0) {
      return 0.0;  // Return 0 if slope calculation would be undefined
    }

    // Calculate slope using least squares method
    double slope = (n * sumXY - sumX * sumY) / denominator;

    // Normalize the slope by the average value to get a percentage
    double avgValue = sumY / n;
    if (avgValue == 0) {
      return 0.0;  // Avoid division by zero in normalization
    }

    double normalizedSlope = (slope / avgValue) * 100;

    // Final NaN check
    return Double.isNaN(normalizedSlope) ? 0.0 : normalizedSlope;
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

  public static Signal fillLongShortLists(Signal signal, Signal lastSignal,
                                          List<Signal> signalsShort,
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

  public static Signal createSignal(Signal signal, SignalType signalType) {
    return new Signal(
        signal.getDate(),
        signal.getPrice(),
        signalType, signal.getComment());
  }

  public static Signal createSignal(BasePrice price, SignalType signalType, String comment) {
    return new Signal(price.getId().getDate(), price.getClose(), signalType, comment);
  }

  public static Signal createSignal(BasePrice price, SignalType signalType, double precizePrice,
                                    String comment) {
    return new Signal(price.getId().getDate(), precizePrice, signalType, comment);
  }

  public static List<String> addSuffix(List<String> tickers, String suffix) {
    return tickers.stream().map(ticker -> ticker + suffix).toList();
  }

  public static LocalDateTime getDateBeforeTimeFrames(LocalDateTime startDate, TimeFrame timeFrame,
                                                      int numberOfTimeFrames) {
    return switch (timeFrame) {
      case MIN5 -> startDate.minusMinutes(5L * numberOfTimeFrames);
      case HOUR -> startDate.minusHours(numberOfTimeFrames);
      case DAY -> startDate.minusDays(numberOfTimeFrames);
      case WEEK -> startDate.minusWeeks(numberOfTimeFrames);
      case MONTH -> startDate.minusMonths(numberOfTimeFrames);
    };
  }

  public static Map<String, OptParam> getOptParamsAsMap(List<OptParam> optParamList) {
    Map<String, OptParam> optParams = new HashMap<>();
    for (OptParam optParam : optParamList) {
      optParams.put(optParam.getId().getParam(), optParam);
    }
    return optParams;
  }
}