package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.OptParamDao;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.Trend;
import korshak.com.screener.dao.TrendRepository;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("StopLossLessThanPrevMinExtremumStrategy")
public class StopLossLessThanPrevMinExtremumStrategy extends BaseStrategy {
  TrendRepository trendRepository;
  List<Trend> trends;
  int deepOfStopLoss = 1;
  List<Double> recentExtremes = new LinkedList<>();
  int iTrends = 0;

  public StopLossLessThanPrevMinExtremumStrategy(PriceDao priceDao,
                                                 TrendRepository trendRepository,
                                                 OptParamDao optParamDao) {
    super(priceDao, optParamDao);
    this.trendRepository = trendRepository;
  }

  @Override
  public Strategy init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                       LocalDateTime endDate) {
    super.init(ticker, timeFrame, startDate, endDate);
    trends = trendRepository.findByIdTickerAndIdTimeframeAndIdDateBetweenOrderByIdDateAsc(ticker,
        timeFrame, startDate, endDate);
    for (iTrends = 0; iTrends < deepOfStopLoss; iTrends++) {
      recentExtremes.add(trends.get(iTrends).getMinExtremum());
    }
    return this;
  }

  @Override
  public Signal getSignal(BasePrice price) {
    if (!trends.get(iTrends).getId().getDate().isBefore(price.getId().getDate())) {
      return null;
    }
    if (iTrends < trends.size() - 1
        && trends.get(iTrends + 1).getId().getDate().isBefore(price.getId().getDate())) {
      iterateOverTrend();
    }
    Double prevLow = Collections.min(recentExtremes);
    if (price.getLow() < prevLow) {
      Signal signal = Utils.createSignal(price, SignalType.LongClose, prevLow,
          "close cause less than prevLow = " + prevLow);
      return signal;
    }
    return null;
  }

  private void iterateOverTrend() {
    recentExtremes.removeFirst();
    iTrends++;
    recentExtremes.add(trends.get(iTrends).getMinExtremum());
  }

  @Override
  public Signal getSignal(BasePrice priceOfBackupTimeframe, BasePrice price) {
    return getSignal(price);
  }

  public int getDeepOfStopLoss() {
    return deepOfStopLoss;
  }

  public void setDeepOfStopLoss(int deepOfStopLoss) {
    this.deepOfStopLoss = deepOfStopLoss;
  }
}
