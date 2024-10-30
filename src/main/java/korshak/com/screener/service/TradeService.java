package korshak.com.screener.service;

import java.time.LocalDateTime;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.vo.StrategyResult;

public interface TradeService {

  StrategyResult calculateProfitAndDrawdown(Strategy strategy,
                                            String ticker,
                                            TimeFrame timeFrame);

  StrategyResult calculateProfitAndDrawdown(Strategy strategy,
                                            String ticker,
                                            LocalDateTime startDate,
                                            LocalDateTime endDate,
                                            TimeFrame timeFrame);

}
