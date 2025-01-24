package korshak.com.screener.utils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.SmaDay;
import korshak.com.screener.dao.SmaHour;
import korshak.com.screener.dao.SmaMonth;
import korshak.com.screener.dao.SmaWeek;
import korshak.com.screener.dao.TimeFrame;

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
}
