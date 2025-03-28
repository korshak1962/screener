package korshak.com.screener.serviceImpl.strategy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import korshak.com.screener.dao.OptParam;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.TradeService;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.vo.StrategyResult;
import org.springframework.stereotype.Service;

@Service("GenericOptimizator")
public class GenericOptimizator extends Optimizator {

  // Member variables for optimization state
  private List<Strategy> strategiesToOptimize;
  private Map<Strategy, Map<String, OptParam>> currentParams;
  private Map<Strategy, Map<String, OptParam>> bestParams;
  private double bestOverallPnL;
  private long combinationsTested;
  private long totalCombinations;

  // Track progress statistics
  private long lastProgressReport = 0;
  private long progressReportInterval = 100;
  private long startTime;

  public GenericOptimizator(StrategyMerger merger, TradeService tradeService) {
    super(merger, tradeService);
  }

  @Override
  public Map<String, Double> findOptimumParameters() {
    // Get optimal parameters for all strategies
    Map<Strategy, Map<String, OptParam>> optimalParamsByStrategy = findOptimalParametersForAllStrategies();

    // Convert to a flat map of parameter names to values for compatibility with existing code
    Map<String, Double> result = new HashMap<>();
    double maxPnL = -Double.MAX_VALUE;

    // Apply the optimal parameters to all strategies and calculate the final PnL
    for (Map.Entry<Strategy, Map<String, OptParam>> entry : optimalParamsByStrategy.entrySet()) {
      Strategy strategy = entry.getKey();

      // Apply optimal parameters
      strategy.setOptParams(entry.getValue());

      // Add parameters to result map
      for (OptParam param : entry.getValue().values()) {
        String paramKey = strategy.getStrategyName() + "_" + param.getId().getParam();
        result.put(paramKey, param.getValue());
      }
    }

    // Run merger with optimal parameters to get final PnL
    merger.mergeSignals();
    StrategyResult finalResult = tradeService.calculateProfitAndDrawdownLong(merger);
    if (finalResult != null) {
      maxPnL = finalResult.getLongPnL();
    }

    // Add max PnL to result
    result.put(MAX_PNL, maxPnL);

    return result;
  }

  /**
   * Finds the optimal parameters for all strategies by testing combinations
   * to maximize the overall PnL of the merged strategy
   * @return Map of strategies to their optimal parameters
   */
  public Map<Strategy, Map<String, OptParam>> findOptimalParametersForAllStrategies() {
    // Initialize member variables
    initializeOptimization();
    startTime = System.currentTimeMillis();

    if (strategiesToOptimize.isEmpty()) {
      System.out.println("No parameters to optimize");
      return bestParams;
    }

    // Calculate total combinations
    totalCombinations = calculateTotalCombinations();
    System.out.println("Total parameter combinations to check: " + totalCombinations);

    // Prepare parameter structures for recursive optimization
    Map<Strategy, List<String>> strategyParamNames = new HashMap<>();
    Map<Strategy, List<List<OptParam>>> strategyParamValues = new HashMap<>();

    // For each strategy, identify all parameters and their potential values
    for (Strategy strategy : strategiesToOptimize) {
      strategyParamNames.put(strategy, new ArrayList<>());
      strategyParamValues.put(strategy, new ArrayList<>());

      for (Map.Entry<String, OptParam> entry : strategy.getOptParams().entrySet()) {
        String paramName = entry.getKey();
        OptParam baseParam = entry.getValue();

        // Add parameter name
        strategyParamNames.get(strategy).add(paramName);

        // Generate all possible values for this parameter
        List<OptParam> paramValues = new ArrayList<>();
        double paramValue = baseParam.getMin();
        while (paramValue <= baseParam.getMax()) {
          // Create param with this value
          OptParam newParam = new OptParam(
              baseParam.getId().getTicker(),
              baseParam.getId().getParam(),
              baseParam.getId().getStrategy(),
              baseParam.getId().getCaseId(),
              baseParam.getTimeframe(),
              paramValue,
              baseParam.getValueString(),
              baseParam.getMin(),
              baseParam.getMax(),
              baseParam.getStep()
          );
          paramValues.add(newParam);
          paramValue += baseParam.getStep();
        }

        // Add parameter values list
        strategyParamValues.get(strategy).add(paramValues);
      }
    }

    // Start recursive optimization with the first strategy and first parameter
    optimizeStrategyRecursive(0, 0, strategyParamNames, strategyParamValues);

    // Apply best parameters to all strategies
    for (Strategy strategy : strategiesToOptimize) {
      strategy.setOptParams(bestParams.get(strategy));
    }

    long totalTimeMs = System.currentTimeMillis() - startTime;

    System.out.println("Optimization complete. Found best overall PnL: " + bestOverallPnL);
    System.out.println("Tested " + combinationsTested + " combinations in " + (totalTimeMs / 1000) + " seconds");
    System.out.println("Best parameters:");
    for (Strategy strategy : strategiesToOptimize) {
      System.out.println("Strategy: " + strategy.getStrategyName());
      for (Map.Entry<String, OptParam> entry : bestParams.get(strategy).entrySet()) {
        System.out.println("  " + entry.getKey() + " = " + entry.getValue().getValue());
      }
    }
    return bestParams;
  }

  /**
   * Initialize all optimization state variables
   */
  private void initializeOptimization() {
    strategiesToOptimize = new ArrayList<>();
    currentParams = new HashMap<>();
    bestParams = new HashMap<>();
    bestOverallPnL = -Double.MAX_VALUE;
    combinationsTested = 0;
    lastProgressReport = 0;

    // Identify strategies that have optimizable parameters
    for (Strategy strategy : merger.getNameToStrategy().values()) {
      if (!strategy.getOptParams().isEmpty()) {
        strategiesToOptimize.add(strategy);
        // Initialize parameter maps
        currentParams.put(strategy, new HashMap<>(strategy.getOptParams()));
        bestParams.put(strategy, new HashMap<>(strategy.getOptParams()));
      }
    }

    // Add merger itself if it has parameters
    if (!merger.getOptParams().isEmpty()) {
      strategiesToOptimize.add(merger);
      currentParams.put(merger, new HashMap<>(merger.getOptParams()));
      bestParams.put(merger, new HashMap<>(merger.getOptParams()));
    }
  }

  /**
   * Recursively optimizes strategies, exploring all combinations of all parameters
   * across all strategies and evaluating the overall PnL after each combination.
   *
   * @param strategyIndex Index of current strategy being optimized
   * @param paramIndex Index of current parameter being optimized for the current strategy
   * @param strategyParamNames Map of strategy to list of parameter names
   * @param strategyParamValues Map of strategy to list of parameter value lists
   */
  private void optimizeStrategyRecursive(
      int strategyIndex,
      int paramIndex,
      Map<Strategy, List<String>> strategyParamNames,
      Map<Strategy, List<List<OptParam>>> strategyParamValues) {

    // Base case: we've gone through all strategies
    if (strategyIndex >= strategiesToOptimize.size()) {
      // Apply all current parameters
      for (Strategy strategy : strategiesToOptimize) {
        strategy.setOptParams(currentParams.get(strategy));
      }

      // Run the merger to calculate overall PnL
      merger.mergeSignals();
      StrategyResult result = tradeService.calculateProfitAndDrawdownLong(merger);
      combinationsTested++;

      // Check if this gives better PnL
      if (result != null && result.getLongPnL() > bestOverallPnL) {
        bestOverallPnL = result.getLongPnL();

        // Store these parameters as best so far
        for (Strategy strategy : strategiesToOptimize) {
          bestParams.get(strategy).clear();
          bestParams.get(strategy).putAll(currentParams.get(strategy));
        }

        System.out.println("New best overall PnL: " + bestOverallPnL);
        for (Strategy strategy : strategiesToOptimize) {
          System.out.println("Strategy: " + strategy.getStrategyName());
          for (Map.Entry<String, OptParam> param : currentParams.get(strategy).entrySet()) {
            System.out.println("  " + param.getKey() + " = " + param.getValue().getValue());
          }
        }
      }

      // Log progress periodically
      if (combinationsTested - lastProgressReport >= progressReportInterval ||
          combinationsTested == totalCombinations) {
        lastProgressReport = combinationsTested;
        long elapsedTimeMs = System.currentTimeMillis() - startTime;
        double percentComplete = (double)combinationsTested / totalCombinations * 100;

        // Estimate remaining time
        long estimatedTotalTimeMs = 0;
        if (combinationsTested > 0) {
          estimatedTotalTimeMs = (long)(elapsedTimeMs * totalCombinations / combinationsTested);
        }
        long remainingTimeMs = estimatedTotalTimeMs - elapsedTimeMs;

        System.out.println(String.format(
            "Progress: %.2f%% (%d/%d) | Elapsed: %ds | Remaining: %ds | Current best PnL: %.2f",
            percentComplete,
            combinationsTested,
            totalCombinations,
            elapsedTimeMs / 1000,
            remainingTimeMs / 1000,
            bestOverallPnL));
      }

      return;
    }

    Strategy currentStrategy = strategiesToOptimize.get(strategyIndex);
    List<String> paramNames = strategyParamNames.get(currentStrategy);

    // If this strategy has no parameters or we've gone through all parameters
    if (paramNames.isEmpty() || paramIndex >= paramNames.size()) {
      // Move to the next strategy
      optimizeStrategyRecursive(strategyIndex + 1, 0, strategyParamNames, strategyParamValues);
      return;
    }

    // Get current parameter name and its possible values
    String paramName = paramNames.get(paramIndex);
    List<OptParam> paramValues = strategyParamValues.get(currentStrategy).get(paramIndex);

    // Try each value for the current parameter
    for (OptParam paramValue : paramValues) {
      // Set this parameter value
      currentParams.get(currentStrategy).put(paramName, paramValue);

      // If this is the last parameter for this strategy, move to next strategy
      if (paramIndex == paramNames.size() - 1) {
        optimizeStrategyRecursive(strategyIndex + 1, 0, strategyParamNames, strategyParamValues);
      } else {
        // Otherwise, move to next parameter for this strategy
        optimizeStrategyRecursive(strategyIndex, paramIndex + 1, strategyParamNames, strategyParamValues);
      }
    }

    // Reset parameter to original value before continuing
    currentParams.get(currentStrategy).put(paramName, currentStrategy.getOptParams().get(paramName));
  }

  /**
   * Calculates the total number of parameter combinations across all strategies
   */
  private long calculateTotalCombinations() {
    long total = 1;

    for (Strategy strategy : strategiesToOptimize) {
      Map<String, OptParam> params = strategy.getOptParams();
      long strategyTotal = 1;

      // Consider all parameters for this strategy
      for (OptParam param : params.values()) {
        long numValues = 1 + Math.round((param.getMax() - param.getMin()) / param.getStep());
        strategyTotal *= numValues;
      }

      total *= strategyTotal;
    }

    return total;
  }

  @Override
  public Map<String, Double> findOptimumParametersWithStopLoss(double minPercent, double maxPercent, double step) {
    // Store original stop loss value
    double originalStopLoss = merger.getStopLossMaxPercent();

    Map<String, Double> bestParams = null;
    double bestPnL = -Double.MAX_VALUE;

    // Try different stop loss values
    for (double stopLoss = minPercent; stopLoss <= maxPercent; stopLoss += step) {
      System.out.println("Testing stop loss: " + stopLoss);

      // Set stop loss
      merger.setStopLossPercent(stopLoss);

      // Find optimal parameters with this stop loss
      Map<String, Double> params = findOptimumParameters();
      double pnl = params.getOrDefault(MAX_PNL, -Double.MAX_VALUE);

      // Update best parameters if better
      if (pnl > bestPnL) {
        bestPnL = pnl;
        bestParams = new HashMap<>(params);
        bestParams.put(STOP_LOSS, stopLoss);
      }
    }

    // Restore original stop loss
    merger.setStopLossPercent(originalStopLoss);

    return bestParams;
  }
}