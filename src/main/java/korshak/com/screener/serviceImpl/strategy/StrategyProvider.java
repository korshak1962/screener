package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.strategy.Configurable;
import korshak.com.screener.service.strategy.Strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class StrategyProvider {
  private final ApplicationContext applicationContext;
  String ticker;
  LocalDateTime startDate;
  LocalDateTime endDate;
  TimeFrame timeFrame;

  @Autowired
  public StrategyProvider(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public void init(String ticker, LocalDateTime startDate,
                   LocalDateTime endDate,
                   TimeFrame timeFrame) {
    this.ticker = ticker;
    this.startDate = startDate;
    this.endDate = endDate;
    this.timeFrame = timeFrame;
  }

  public Strategy getStrategy(String strategyClassName) {
    try {
      // Get the fully qualified class name if only simple name is provided
      if (!strategyClassName.contains(".")) {
        strategyClassName = "korshak.com.screener.serviceImpl.strategy." + strategyClassName;
      }
      // Get the class object for the strategy
      Class<?> strategyClass = Class.forName(strategyClassName);
      // Verify it implements the Strategy interface
      if (!Strategy.class.isAssignableFrom(strategyClass)) {
        throw new IllegalArgumentException(
            "The provided class does not implement the Strategy interface: " + strategyClassName);
      }
      // Get the bean from Spring context
      Strategy strategy = (Strategy) applicationContext.getBean(strategyClass);
      strategy.init(ticker, timeFrame, startDate, endDate);
      return strategy;
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Strategy class not found: " + strategyClassName, e);
    }
  }

  // Overload to accept Class directly
  public Strategy getStrategy(Class<? extends Strategy> strategyClass) {
    Strategy strategy = applicationContext.getBean(strategyClass);
    //strategy.init(ticker, timeFrame, startDate, endDate);
    return strategy;
  }
}