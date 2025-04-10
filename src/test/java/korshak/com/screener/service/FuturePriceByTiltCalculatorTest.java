package korshak.com.screener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceDay;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.SmaDao;
import korshak.com.screener.dao.SmaDay;
import korshak.com.screener.dao.SmaKey;
import korshak.com.screener.serviceImpl.calc.FuturePriceByTiltCalculator;
import korshak.com.screener.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FuturePriceByTiltCalculatorTest {
  private final int SMA_LENGTH = 3;
  private final double TARGET_TILT = .02;
  @Mock
  private PriceDao priceDao;
  @Mock
  private SmaDao smaDao;
  private FuturePriceByTiltCalculator calculator;
  private List<BasePrice> prices;
  private List<BaseSma> smas;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    calculator = new FuturePriceByTiltCalculator(priceDao, smaDao);
    setupTestData();
  }

  private void setupTestData() {
    // Setup base prices
    prices = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    for (int i = 0; i < SMA_LENGTH - 1; i++) {
      PriceDay price = new PriceDay();
      price.setId(new PriceKey("TEST", now.plusDays(i)));
      price.setClose(100.0 + i);
      prices.add(price);
    }

    // Setup SMAs
    smas = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      SmaDay sma = new SmaDay();
      sma.setId(new SmaKey("TEST", now.plusDays(i), SMA_LENGTH));
      sma.setValue(100.0 + i);
      smas.add(sma);
    }
  }

  @Test
  void calculatePriceFromData_ShouldConvergeToTargetTilt() {
    double target_tilt = -0.02;
    double result = calculator.calculatePriceFromData(prices, smas, SMA_LENGTH, target_tilt);
    System.out.println("result = " + result);
    List<BaseSma> verificationSmas = new ArrayList<>(smas);

    // Calculate new SMA with result price
    double sum = prices.stream().mapToDouble(BasePrice::getClose).sum() + result;
    double newSma = sum / SMA_LENGTH;

    // Add new SMA to verification list
    SmaDay newSmaPeriod = new SmaDay();
    newSmaPeriod.setValue(newSma);
    verificationSmas.add(newSmaPeriod);

    // Calculate actual tilt using Utils
    double actualTilt = Utils.calculateTilt(verificationSmas);

    // Verify the result produces the target tilt within acceptable precision
    assertEquals(target_tilt, actualTilt, 0.001,
        "Calculated price should produce SMA with target tilt");
    System.out.println("target_tilt = " + target_tilt + "  actualTilt = " + actualTilt);

  }

  @Test
  void calculatePriceFromData_ShouldThrowException_WhenInputInvalid() {
    // Test with invalid prices size
    List<BasePrice> invalidPrices = prices.subList(0, 1);
    assertThrows(IllegalArgumentException.class, () ->
        calculator.calculatePriceFromData(invalidPrices, smas, SMA_LENGTH, TARGET_TILT));

    // Test with invalid SMAs size
    List<BaseSma> invalidSmas = smas.subList(0, 2);
    assertThrows(IllegalArgumentException.class, () ->
        calculator.calculatePriceFromData(prices, invalidSmas, SMA_LENGTH, TARGET_TILT));
  }
}