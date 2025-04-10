package korshak.com.screener.serviceImpl.calc;

import java.util.List;
import korshak.com.screener.service.calc.RsiService;
import korshak.com.screener.service.calc.TrendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Calculator {

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
    for (int length = startLength; length <= endLength; length += step) {
      smaCalculationService.calculateIncrementalSMAForAllTimeFrames(ticker, length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  private void calcSMA(List<String> tickers, int startLength, int endLength) {
    for (String ticker : tickers) {
      calcSMA(ticker, startLength, endLength);
    }
  }

  private void calcSMA(String ticker, int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      smaCalculationService.calculateSMAForAllTimeFrame(ticker, length);
      System.out.println("length = " + length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    //System.exit(0);
  }

  private void calcRSI(String ticker, int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      rsiService.calculateRsiForAllTimeFrames(ticker, length);
      System.out.println("length = " + length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  private void calcSMA(int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      smaCalculationService.calculateSMAForAllTimeFrameAndTickers(length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  private void calcRSI(int startLength, int endLength) {
    int step = 1;
    long start = System.currentTimeMillis();
    for (int length = startLength; length <= endLength; length += step) {
      rsiService.calculateIncrementalRsiForAllTickersAndTimeFrames(length);
    }
    System.out.println("total in minutes= " + (System.currentTimeMillis() - start) / 60000);
    System.exit(0);
  }

  public void agregateAndSmaCalc(int lengthMin, int lengthMax, String dbTicker) {
    priceAggregationService.aggregateAllTimeFrames(dbTicker);
    for (int i = lengthMin; i <= lengthMax; i++) {
      smaCalculationService.calculateIncrementalSMAForAllTimeFrames(
          dbTicker, i);
    }
  }
}
