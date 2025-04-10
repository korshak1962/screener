package korshak.com.screener.serviceImpl.strategy;

import java.util.ArrayList;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.SmaDao;
import korshak.com.screener.vo.SignalTilt;
import korshak.com.screener.vo.SignalType;
import org.springframework.stereotype.Service;

@Service("BuyAndHoldStrategyMinusDownTrend")
public class BuyAndHoldStrategyMinusDownTrend extends DoubleTiltStrategy {


  public BuyAndHoldStrategyMinusDownTrend(SmaDao smaDao,
                                          PriceDao priceDao) {
    super(smaDao, priceDao);
  }

  @Override
  public String getStrategyName() {
    return "BuyandHoldMinusDownTrend";
  }

  @Override
  public void calcSignals() {
    signalsLong = new ArrayList<>();
    signalsShort = new ArrayList<>();
    double shortTilt = 0.0;
    double longTilt = 0.0;
    for (int i = 0; i < smaLongList.size(); i++) {
      shortTilt = smaShortList.get(i).getTilt();
      longTilt = smaLongList.get(i).getTilt();
      if (longTilt > tiltHigherTrendLong) {
        //close short if have
        if (!signalsShort.isEmpty() &&
            signalsShort.getLast().getSignalType() == SignalType.ShortOpen) {
          signalsShort.add(new SignalTilt(
              prices.get(i).getId().getDate(),
              prices.get(i).getClose(),
              SignalType.ShortClose, shortTilt, longTilt, "B&H minusDownTrend"));
        }
        // then open long
        if (signalsLong.isEmpty() || signalsLong.getLast().getSignalType() != SignalType.LongOpen) {
          signalsLong.add(new SignalTilt(
              prices.get(i).getId().getDate(),
              prices.get(i).getClose(),
              SignalType.LongOpen, shortTilt, longTilt, "B&H minusDownTrend"));
        }
      }

      if (shortTilt > tiltShortClose) {
        //close short
        if (!signalsShort.isEmpty() &&
            signalsShort.getLast().getSignalType() == SignalType.ShortOpen) {
          signalsShort.add(new SignalTilt(
              prices.get(i).getId().getDate(),
              prices.get(i).getClose(),
              SignalType.ShortClose, shortTilt, longTilt, "B&H minusDownTrend"));
        }
      }

      if (longTilt < tiltHigherTrendLong) {
        //close long
        if (!signalsLong.isEmpty() &&
            signalsLong.getLast().getSignalType() == SignalType.LongOpen) {
          signalsLong.add(new SignalTilt(
              prices.get(i).getId().getDate(),
              prices.get(i).getClose(),
              SignalType.LongClose, shortTilt, longTilt, "B&H minusDownTrend"));
        }

        if (shortTilt < tiltShortOpen && longTilt < tiltHigherTrendShort) {
          // open short
          if (signalsShort.isEmpty() ||
              signalsShort.getLast().getSignalType() != SignalType.ShortOpen) {
            signalsShort.add(new SignalTilt(
                prices.get(i).getId().getDate(),
                prices.get(i).getClose(),
                SignalType.ShortOpen, shortTilt, longTilt, "B&H minusDownTrend"));
          }
        }
      }
    }
    if (!signalsLong.isEmpty()) {
      SignalTilt lastLong = (SignalTilt)signalsLong.getLast();
      if (lastLong.getSignalType() == SignalType.LongOpen) {
        System.out.println("======== lastLong Signal " + lastLong);
      }
    }
    if (!signalsShort.isEmpty()) {
      SignalTilt lastShort = (SignalTilt)signalsShort.getLast();
      if (lastShort.getSignalType() == SignalType.ShortOpen) {
        System.out.println("======== lastShort Signal " + lastShort);
      }
    }
    System.out.println(
        "======== shortTilt = " + shortTilt + " for date = " + prices.getLast().getId().getDate());
    System.out.println("======== longTilt = " + longTilt);
  }

}
