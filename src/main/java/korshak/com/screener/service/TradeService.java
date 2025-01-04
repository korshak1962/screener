package korshak.com.screener.service;

import java.time.LocalDateTime;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.vo.StrategyResult;

public interface TradeService {

  StrategyResult calculateProfitAndDrawdownLong(Strategy strategy);
  StrategyResult calculateProfitAndDrawdownShort(Strategy strategy);
  StrategyResult calculateProfitAndDrawdownLong(Strategy strategy,
                                                String ticker,
                                                LocalDateTime startDate,
                                                LocalDateTime endDate,
                                                TimeFrame timeFrame);

}
