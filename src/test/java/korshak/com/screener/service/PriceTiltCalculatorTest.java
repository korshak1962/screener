package korshak.com.screener.service;

import korshak.com.screener.dao.*;
import korshak.com.screener.serviceImpl.PriceTiltCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PriceTiltCalculatorTest {
  @Mock
  private PriceDao priceDao;

  @Mock
  private SmaDao smaDao;

  private PriceTiltCalculator calculator;
  private static final int SMA_LENGTH = 9;
  private static final int TILT_PERIOD = 5;

  @BeforeEach
  void setUp() {
    calculator = new PriceTiltCalculator(priceDao, smaDao);
  }

  @Test
  void calculatePriceFromData_VerifyTiltCalculation() {
    LocalDateTime now = LocalDateTime.now();
    List<PriceDay> allPrices = createPrices(now, SMA_LENGTH + TILT_PERIOD - 1);
    List<SmaDay> smas = calculateSmas(allPrices, SMA_LENGTH, TILT_PERIOD - 1);
    List<PriceDay> prices = allPrices.subList(allPrices.size() - (SMA_LENGTH - 1), allPrices.size());

    System.out.println("Initial SMAs:");
    smas.forEach(sma -> System.out.printf("Date: %s, Value: %.4f%n",
        sma.getId().getDate(), sma.getValue()));

    System.out.println("\nPrices:");
    prices.forEach(price -> System.out.printf("Date: %s, Close: %.4f%n",
        price.getId().getDate(), price.getClose()));

    double targetTilt = -0.02;
    double calculatedPrice = calculator.calculatePriceFromData(prices, smas, SMA_LENGTH, targetTilt);
    System.out.printf("\nCalculated Price: %.4f%n", calculatedPrice);

    // Calculate new SMA with the calculated price
    double sum = prices.stream().mapToDouble(BasePrice::getClose).sum() + calculatedPrice;
    double newSma = sum / SMA_LENGTH;
    System.out.printf("New SMA: %.4f%n", newSma);

    // Add new SMA to list and verify tilt
    List<Double> smaValues = new ArrayList<>();
    smas.forEach(sma -> smaValues.add(sma.getValue()));
    smaValues.add(newSma);

    System.out.println("\nAll SMA values for tilt calculation:");
    for (int i = 0; i < smaValues.size(); i++) {
      System.out.printf("Position %d: %.4f%n", i, smaValues.get(i));
    }

    double calculatedTilt = calculateTilt(smaValues);
    System.out.printf("\nExpected tilt: %.4f%n", targetTilt);
    System.out.printf("Actual tilt: %.4f%n", calculatedTilt);

    assertEquals(targetTilt, calculatedTilt, 0.001,
        String.format("Expected tilt: %.4f, but got: %.4f", targetTilt, calculatedTilt));
  }


  @Test
  void calculatePriceFromData_NotEnoughSmas_ThrowsException() {
    LocalDateTime now = LocalDateTime.now();
    List<PriceDay> allPrices = createPrices(now, SMA_LENGTH + TILT_PERIOD - 1);
    List<SmaDay> smas = calculateSmas(allPrices, SMA_LENGTH, TILT_PERIOD - 2);
    List<PriceDay> prices = allPrices.subList(allPrices.size() - (SMA_LENGTH - 1), allPrices.size());

    assertThrows(IllegalArgumentException.class,
        () -> calculator.calculatePriceFromData(prices, smas, SMA_LENGTH, -0.02));
  }

  private List<PriceDay> createPrices(LocalDateTime baseTime, int count) {
    List<PriceDay> prices = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      PriceDay price = new PriceDay();
      price.setId(new PriceKey("TEST", baseTime.plusDays(i)));
      // Generate prices that create a gentler slope
      price.setClose(100.0 + (i * 0.1));
      prices.add(price);
    }
    return prices;
  }

  private List<SmaDay> calculateSmas(List<PriceDay> prices, int smaLength, int count) {
    if (prices.size() < smaLength + count - 1) {
      throw new IllegalArgumentException("Not enough prices to calculate required SMAs");
    }

    List<SmaDay> smas = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      int startIdx = prices.size() - count - smaLength + i;
      double sum = 0;
      for (int j = 0; j < smaLength; j++) {
        sum += prices.get(startIdx + j).getClose();
      }

      SmaDay sma = new SmaDay();
      sma.setId(new SmaKey("TEST", prices.get(startIdx + smaLength - 1).getId().getDate(), smaLength));
      sma.setValue(sum / smaLength);
      smas.add(sma);
    }

    return smas;
  }

  private double calculateTilt(List<Double> smaValues) {
    int n = smaValues.size();
    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumXX = 0;

    for (int i = 0; i < n; i++) {
      sumX += i;
      sumY += smaValues.get(i);
      sumXY += i * smaValues.get(i);
      sumXX += i * i;
    }

    double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    double avgSma = sumY / n;
    return (slope / avgSma) * 100;
  }
}