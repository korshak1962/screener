package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.OptParamDao;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.SmaDao;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.utils.Utils;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("TiltFromBaseStrategy")
public class TiltFromBaseStrategy extends BaseStrategy {

  final SmaDao smaDao;
  Map<LocalDateTime, BaseSma> smaMap;
  double tiltBuy = 1;
  double tiltSell = -1;
  int length;
  List<? extends BaseSma> smaList;

  public TiltFromBaseStrategy(SmaDao smaDao, PriceDao priceDao, OptParamDao optParamDao) {
    super(priceDao, optParamDao);
    this.smaDao = smaDao;
  }

  @Override
  public Signal getSignal(BasePrice price) {
    Signal signalToAdd = null;
    BaseSma currentSma = smaMap.get(price.getId().getDate());
    if (currentSma == null) {
      return signalToAdd;
    }
    if (currentSma.getTilt() > tiltBuy) {
      signalToAdd =
          Utils.createSignal(price, SignalType.LongOpen, "TiltBuy = " + currentSma.getTilt());
    } else if (currentSma.getTilt() < tiltSell) {
      signalToAdd =
          Utils.createSignal(price, SignalType.ShortOpen, "tiltSell = " + currentSma.getTilt());
    }
    return signalToAdd;
  }

  @Override
  public Signal getSignal(BasePrice priceOfBackupTimeframe, BasePrice price) {


    return null;
  }

  public double getTiltBuy() {
    return tiltBuy;
  }

  public void setTiltBuy(double tiltBuy) {
    this.tiltBuy = tiltBuy;
  }

  public double getTiltSell() {
    return tiltSell;
  }

  public void setTiltSell(double tiltSell) {
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
      throw new RuntimeException(
          "No SMAs found for ticker = " + ticker + " length = " + length + " timeframe = " +
              timeFrame
              + " startDate = " + startDate + " endDate = " + endDate);
    }
    smaMap = smaList.stream()
        .collect(Collectors.toMap(
            sma -> sma.getId().getDate(),
            sma -> sma
        ));
  }

  @Override
  public List<Signal> getAllSignals(TimeFrame signalTimeFrame) {
    if (signalTimeFrame.ordinal() == this.timeFrame.ordinal()) {
      return getAllSignals();
    }
    throw new IllegalArgumentException("Wrong time frame for signal");
  }

  public void setOptParams() {
    optParams = optParamDao.findValuesByTickerAndTimeframeAndStrategy(ticker, timeFrame,
        this.getClass().getSimpleName());
    if (optParams != null && optParams.get("Length") != null) {
      setLength(optParams.get("Length").intValue());
      tiltBuy = optParams.get("TiltBuy");
      tiltSell = optParams.get("TiltSell");
    }else{
      throw new RuntimeException("No opt params for strategy = " + this.getClass().getSimpleName() +
          " ticker = " + ticker + " timeframe = " + timeFrame);
    }
  }
}
