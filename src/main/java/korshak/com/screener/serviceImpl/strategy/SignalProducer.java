package korshak.com.screener.serviceImpl.strategy;

import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.vo.Signal;

public interface SignalProducer {
  Signal getSignal(BasePrice price);
}
