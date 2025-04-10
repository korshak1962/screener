package korshak.com.screener.serviceImpl.calc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import korshak.com.screener.service.calc.RsiService;
import korshak.com.screener.service.calc.TrendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Calculator {

  private static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() - 2;

  @Autowired
  RsiService rsiService;
  @Autowired
  private TrendService trendService;
  @Autowired
  private FuturePriceByTiltCalculator futurePriceByTiltCalculator;
  @Autowired
  private SmaCalculationServiceImpl smaCalculationService;
  @Autowired
  private PriceAggregationServiceImpl priceAggregationService;


  private void calcSMA_incremental(String ticker, int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();

    // Create thread pool
    ExecutorService executor = Executors.newFixedThreadPool(Math.min(endLength - startLength + 1, MAX_THREADS));
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int length = startLength; length <= endLength; length += step) {
      final int currentLength = length;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        System.out.println(Thread.currentThread().getName() + " - Processing SMA length " + currentLength);
        smaCalculationService.calculateIncrementalSMAForAllTimeFrames(ticker, currentLength);
      }, executor);

      futures.add(future);
    }

    // Wait for all tasks to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // Shutdown the executor
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  private void calcSMA(List<String> tickers, int startLength, int endLength) {
    // Create thread pool
    ExecutorService executor = Executors.newFixedThreadPool(Math.min(tickers.size(), MAX_THREADS));
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (String ticker : tickers) {
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        calcSMA(ticker, startLength, endLength);
      }, executor);

      futures.add(future);
    }

    // Wait for all tasks to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // Shutdown the executor
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private void calcSMA(String ticker, int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();

    // Create thread pool
    ExecutorService executor = Executors.newFixedThreadPool(Math.min(endLength - startLength + 1, MAX_THREADS));
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int length = startLength; length <= endLength; length += step) {
      final int currentLength = length;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        smaCalculationService.calculateSMAForAllTimeFrame(ticker, currentLength);
        System.out.println("length = " + currentLength);
      }, executor);

      futures.add(future);
    }

    // Wait for all tasks to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // Shutdown the executor
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
  }

  private void calcRSI(String ticker, int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();

    // Create thread pool
    ExecutorService executor = Executors.newFixedThreadPool(Math.min(endLength - startLength + 1, MAX_THREADS));
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int length = startLength; length <= endLength; length += step) {
      final int currentLength = length;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        rsiService.calculateRsiForAllTimeFrames(ticker, currentLength);
        System.out.println("length = " + currentLength);
      }, executor);

      futures.add(future);
    }

    // Wait for all tasks to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // Shutdown the executor
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  private void calcSMA(int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();

    // Create thread pool
    ExecutorService executor = Executors.newFixedThreadPool(Math.min(endLength - startLength + 1, MAX_THREADS));
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int length = startLength; length <= endLength; length += step) {
      final int currentLength = length;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        smaCalculationService.calculateSMAForAllTimeFrameAndTickers(currentLength);
      }, executor);

      futures.add(future);
    }

    // Wait for all tasks to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // Shutdown the executor
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  private void calcRSI(int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();

    // Create thread pool
    ExecutorService executor = Executors.newFixedThreadPool(Math.min(endLength - startLength + 1, MAX_THREADS));
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int length = startLength; length <= endLength; length += step) {
      final int currentLength = length;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        rsiService.calculateIncrementalRsiForAllTickersAndTimeFrames(currentLength);
      }, executor);

      futures.add(future);
    }

    // Wait for all tasks to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // Shutdown the executor
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  public void agregateAndSmaCalc(int lengthMin, int lengthMax, String dbTicker) {
    priceAggregationService.aggregateAllTimeFrames(dbTicker);

    // Create thread pool
    ExecutorService executor = Executors.newFixedThreadPool(Math.min(lengthMax - lengthMin + 1, MAX_THREADS));
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int i = lengthMin; i <= lengthMax; i++) {
      final int length = i;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        System.out.println(Thread.currentThread().getName() + " - Processing SMA length " + length + " for " + dbTicker);
        smaCalculationService.calculateIncrementalSMAForAllTimeFrames(dbTicker, length);
      }, executor);

      futures.add(future);
    }

    // Wait for all tasks to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // Shutdown the executor
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    System.out.println("Completed SMA calculations for all lengths for " + dbTicker);
  }
}