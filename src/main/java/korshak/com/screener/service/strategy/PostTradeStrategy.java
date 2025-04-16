package korshak.com.screener.service.strategy;

import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.vo.Signal;

public interface PostTradeStrategy extends Configurable {
  Signal getPostTradeSignal(BasePrice price, Signal lastSignal);
}
