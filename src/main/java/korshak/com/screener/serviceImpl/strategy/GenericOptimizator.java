package korshak.com.screener.serviceImpl.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import korshak.com.screener.dao.Param;
import korshak.com.screener.service.calc.TradeService;
import korshak.com.screener.service.strategy.Configurable;
import korshak.com.screener.service.strategy.Strategy;
import korshak.com.screener.vo.StrategyResult;
import org.springframework.stereotype.Service;

@Service("GenericOptimizator")
public class GenericOptimizator {

  StrategyMerger merger;
  TradeService tradeService;
  private List<Configurable> strategiesToOptimize;
  private Map<Configurable, Map<String, Param>> currentParams;
  private Map<Configurable, Map<String, Param>> bestParams;
  private double bestFunctionalResult;
  private StrategyResult bestOverallResult;
  private long combinationsTested;
  private long totalCombinations;

  // Parameter structures for optimization
  private Map<Configurable, List<String>> strategyParamNames;
  private Map<Configurable, List<List<Param>>> strategyParamValues;

  // Track progress statistics
  private long lastProgressReport = 0;
  private long progressReportInterval = 100;
  private long startTime;

  public GenericOptimizator(StrategyMerger merger, TradeService tradeService) {
    this.merger = merger;
    this.tradeService = tradeService;
  }

  /**
   * Finds the optimal parameters for all strategies by testing combinations
   * to maximize the overall PnL of the merged strategy
   *
   * @return Map of strategies to their optimal parameters
   */
  public Map<Configurable, Map<String, Param>> findOptimalParametersForAllStrategies() {
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

    // Initialize parameter structures for recursive optimization
    strategyParamNames = new HashMap<>();
    strategyParamValues = new HashMap<>();

    // For each strategy, identify all parameters and their potential values
    for (Configurable strategy : strategiesToOptimize) {
      strategyParamNames.put(strategy, new ArrayList<>());
      strategyParamValues.put(strategy, new ArrayList<>());

      for (Map.Entry<String, Param> entry : strategy.getParams().entrySet()) {
        String paramName = entry.getKey();
        Param baseParam = entry.getValue();

        // Add parameter name
        strategyParamNames.get(strategy).add(paramName);

        // Generate all possible values for this parameter
        List<Param> paramValues = new ArrayList<>();
        double paramValue = baseParam.getMin();
        while (paramValue <= baseParam.getMax()) {
          //printMemory("paramValue = "+ paramValue);
          // Create param with this value
          Param newParam = new Param(
              baseParam.getId().getTicker(),
              baseParam.getId().getParam(),
              baseParam.getId().getStrategy(),
              baseParam.getId().getCaseId(),
              baseParam.getTimeframe(),
              baseParam.getStrategyClass(),
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
    optimizeStrategyRecursive(0, 0);

    // Apply best parameters to all strategies
    for (Configurable strategy : strategiesToOptimize) {
      strategy.configure(bestParams.get(strategy));
    }

    long totalTimeMs = System.currentTimeMillis() - startTime;

    System.out.println("Optimization complete. Found best overall PnL: " + bestFunctionalResult);
    System.out.println(
        "Tested " + combinationsTested + " combinations in " + (totalTimeMs / 1000) + " seconds");
    System.out.println("Best parameters:");
    for (Configurable strategy : strategiesToOptimize) {
      for (Map.Entry<String, Param> entry : bestParams.get(strategy).entrySet()) {
        System.out.println("  " + entry.getKey() + " = " + entry.getValue().getValue());
      }
    }
    return bestParams;
  }

  /**
   * Initialize all optimization state variables
   */
  private void initializeOptimization() {
    printMemory("initializeOptimization");
    strategiesToOptimize = new ArrayList<>();
    currentParams = new HashMap<>();
    bestParams = new HashMap<>();
    bestFunctionalResult = -Double.MAX_VALUE;
    combinationsTested = 0;
    lastProgressReport = 0;
    strategyParamNames = new HashMap<>();
    strategyParamValues = new HashMap<>();

    // Identify strategies that have optimizable parameters
    for (Configurable strategy : merger.getConfigurables()) {
      if (!strategy.getParams().isEmpty()) {
        strategiesToOptimize.add(strategy);
        // Initialize parameter maps
        currentParams.put(strategy, new HashMap<>(strategy.getParams()));
        bestParams.put(strategy, new HashMap<>(strategy.getParams()));
      }
    }
  }

  private static void printMemory(String tag) {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    System.out.println(tag + " Memory usage before optimization: " + usedMemory + " MB");
  }

  /**
   * Recursively optimizes strategies, exploring all combinations of all parameters
   * across all strategies and evaluating the overall PnL after each combination.
   *
   * @param strategyIndex Index of current strategy being optimized
   * @param paramIndex    Index of current parameter being optimized for the current strategy
   */
  private void optimizeStrategyRecursive(int strategyIndex, int paramIndex) {

    // Base case: we've gone through all strategies
    if (strategyIndex >= strategiesToOptimize.size()) {
      // Apply all current parameters
      for (Configurable strategy : strategiesToOptimize) {
        strategy.configure(currentParams.get(strategy));
      }

      // Run the merger to calculate overall PnL
      merger.mergeSignals();
      StrategyResult result = tradeService.calculateProfitAndDrawdownLong(merger);
      combinationsTested++;

      // Check if this gives better PnL
      if (result != null) {
        double functionalValue = calcFunctional(result);
        if (functionalValue > bestFunctionalResult) {
          bestFunctionalResult = functionalValue;
          bestOverallResult = result;
          // Store these parameters as best so far
          for (Configurable strategy : strategiesToOptimize) {
            bestParams.get(strategy).clear();
            bestParams.get(strategy).putAll(currentParams.get(strategy));
          }

          System.out.println("New best overall PnL: " + bestFunctionalResult);
          for (Configurable strategy : strategiesToOptimize) {
            for (Map.Entry<String, Param> param : currentParams.get(strategy).entrySet()) {
              System.out.println("  " + param.getKey() + " = " + param.getValue().getValue());
            }
          }
        }
      }

      // Log progress periodically
      if (combinationsTested - lastProgressReport >= progressReportInterval ||
          combinationsTested == totalCombinations) {
        lastProgressReport = combinationsTested;
        long elapsedTimeMs = System.currentTimeMillis() - startTime;
        double percentComplete = (double) combinationsTested / totalCombinations * 100;

        // Estimate remaining time
        long estimatedTotalTimeMs = 0;
        if (combinationsTested > 0) {
          estimatedTotalTimeMs = (long) (elapsedTimeMs * totalCombinations / combinationsTested);
        }
        long remainingTimeMs = estimatedTotalTimeMs - elapsedTimeMs;

        System.out.println(String.format(
            "Progress: %.2f%% (%d/%d) | Elapsed: %ds | Remaining: %ds | Current best PnL: %.2f",
            percentComplete,
            combinationsTested,
            totalCombinations,
            elapsedTimeMs / 1000,
            remainingTimeMs / 1000,
            bestFunctionalResult));
      }

      return;
    }

    Configurable currentStrategy = strategiesToOptimize.get(strategyIndex);
    List<String> paramNames = strategyParamNames.get(currentStrategy);

    // If this strategy has no parameters or we've gone through all parameters
    if (paramNames.isEmpty() || paramIndex >= paramNames.size()) {
      // Move to the next strategy
      optimizeStrategyRecursive(strategyIndex + 1, 0);
      return;
    }

    // Get current parameter name and its possible values
    String paramName = paramNames.get(paramIndex);
    List<Param> paramValues = strategyParamValues.get(currentStrategy).get(paramIndex);

    // Try each value for the current parameter
    for (Param paramValue : paramValues) {
      // Set this parameter value
      currentParams.get(currentStrategy).put(paramName, paramValue);

      // If this is the last parameter for this strategy, move to next strategy
      if (paramIndex == paramNames.size() - 1) {
        optimizeStrategyRecursive(strategyIndex + 1, 0);
      } else {
        // Otherwise, move to next parameter for this strategy
        optimizeStrategyRecursive(strategyIndex, paramIndex + 1);
      }
    }

    // Reset parameter to original value before continuing
    currentParams.get(currentStrategy).put(paramName, currentStrategy.getParams().get(paramName));
  }

  private static double calcFunctional(StrategyResult result) {
    return result.getLongPnL();
  }

  /**
   * Calculates the total number of parameter combinations across all strategies
   */
  private long calculateTotalCombinations() {
    long total = 1;
    for (Configurable strategy : strategiesToOptimize) {
      Map<String, Param> params = strategy.getParams();
      long strategyTotal = 1;
      // Consider all parameters for this strategy
      for (Param param : params.values()) {
        int round = Math.round((param.getMax() - param.getMin() )/ param.getStep());
        round = round > 0 ? round : 0;
        long numValues = 1 + round;
        strategyTotal *= numValues;
      }
      total *= strategyTotal;
    }
    return total;
  }

  public StrategyResult getBestOverallResult() {
    return bestOverallResult;
  }
}