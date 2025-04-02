package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ListIterator;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.OptParamDao;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.Trend;
import korshak.com.screener.service.TrendService;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("TrendChangeStrategy")
@Scope("prototype")
public class TrendChangeStrategy extends BaseStrategy {
  TrendService trendService;
  List<Trend> trends;
  private Trend prevTrend;
  private Trend nextTrend;
  private ListIterator<Trend> trendIterator;

  public TrendChangeStrategy(PriceDao priceDao, TrendService trendService,
                             OptParamDao optParamDao) {
    super(priceDao, optParamDao);
    this.trendService = trendService;
  }

  @Override
  public Strategy init(String ticker, TimeFrame timeFrame, LocalDateTime startDate,
                       LocalDateTime endDate) {
    super.init(ticker, timeFrame, startDate, endDate);
    trends = trendService.findByIdTickerAndIdTimeframeAndIdDateBetweenOrderByIdDateAsc(ticker,
        timeFrame,
        Utils.getDateBeforeTimeFrames(startDate, timeFrame, 5),
        endDate);
    trendIterator = trends.listIterator();
    prevTrend = trendIterator.next();
    nextTrend = trendIterator.next();
    return this;
  }

  @Override
  public Signal getSignal(BasePrice price) {
    Trend trend = getTrendByDate(price.getId().getDate());
    if (trend != null) {
      if (trend.getTrend() == 1) {
        Signal signal = Utils.createSignal(price, SignalType.LongOpen, price.getOpen(),
            "open cause trend = 1 timeframe = " + this.timeFrame);
        return signal;
      }
      if (trend.getTrend() < -1) {
        Signal signal = Utils.createSignal(price, SignalType.ShortOpen, price.getOpen(),
            "open cause trend = -1 timeframe = " + this.timeFrame);
        return signal;
      }
    }
    return null;
  }

  private Trend getTrendByDate(LocalDateTime date) {
    if(prevTrend.getId().getDate().isAfter(date)){
      trendIterator = trends.listIterator();
      prevTrend = trendIterator.next();
      nextTrend = trendIterator.next();
    }
    while (nextTrend.getId().getDate().isBefore(date) ||
        nextTrend.getId().getDate().isEqual(date)) {
      if (trendIterator.hasNext()) {
        prevTrend = nextTrend;
        nextTrend = trendIterator.next();
      } else {
        return nextTrend;
      }
    }
    return prevTrend;
  }

  @Override
  public Signal getSignal(BasePrice priceOfBackupTimeframe, BasePrice price) {
    return getSignal(price);
  }

  @Override
  public TimeFrame getTimeFrame() {
    return timeFrame;
  }

  public void setTimeFrame(TimeFrame timeFrame) {
    this.timeFrame = timeFrame;
  }
}
