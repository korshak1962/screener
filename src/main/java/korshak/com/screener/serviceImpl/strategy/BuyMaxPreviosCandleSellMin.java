package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.vo.Signal;
import org.springframework.stereotype.Service;

@Service("BuyMaxPreviosCandleSellMin")
public class BuyMaxPreviosCandleSellMin implements Strategy {
  private List<Signal> getSignalsLong(List<? extends BasePrice> prices) {
    List<Signal> signals = new ArrayList<>();
    double prevHigh = prices.getFirst().getHigh();
    return signals;
  }

  @Override
  public List<Signal> getSignalsLong() {
    return List.of();
  }

  @Override
  public List<? extends Signal> getSignalsShort() {
    return List.of();
  }

  @Override
  public void init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                   LocalDateTime endDate) {

  }

  @Override
  public String getName() {
    return "BuyMaxPreviosCandleSellMin";
  }

  @Override
  public List<? extends BasePrice> getPrices() {
    return List.of();
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getIndicators() {
    return Map.of();
  }

  @Override
  public Map<String, NavigableMap<LocalDateTime, Double>> getPriceIndicators() {
    return Map.of();
  }

  @Override
  public TimeFrame getTimeFrame() {
    return null;
  }

  @Override
  public String getTicker() {
    return "";
  }

  @Override
  public LocalDateTime getStartDate() {
    return null;
  }

  @Override
  public LocalDateTime getEndDate() {
    return null;
  }
}
