package korshak.com.screener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import korshak.com.screener.dao.BaseSma;

public class Utils {
  public static NavigableMap<LocalDateTime, Double> convertBaseSmaListToTreeMap(List<? extends BaseSma> sortedSmaList){
    NavigableMap<LocalDateTime, Double> map = new TreeMap<>();
    for (BaseSma sma : sortedSmaList) {
      map.put(sma.getId().getDate(), sma.getValue());
    }
    return map;
  }
}
