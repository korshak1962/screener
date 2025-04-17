package korshak.com.screener.service.calc;

import java.time.LocalDateTime;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.serviceImpl.strategy.StrategyMerger;
import korshak.com.screener.vo.StrategyResult;

public interface TradeService {

  StrategyResult calculateProfitAndDrawdownLong(StrategyMerger strategy);

  StrategyResult calculateProfitAndDrawdownShort(StrategyMerger strategy);

  StrategyResult calculateProfitAndDrawdownLong(Strategy strategy);
}
