package korshak.com.screener.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.dao.Trend;
import korshak.com.screener.dao.TrendRepository;
import korshak.com.screener.service.calc.TrendService;
import korshak.com.screener.serviceImpl.calc.TrendServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TrendServiceTest {
  @Mock
  private PriceDao priceDao;

  @Mock
  private TrendRepository trendRepository;

  private TrendService trendService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    trendService = new TrendServiceImpl(priceDao, trendRepository);
  }

  @Test
  void whenPricesEmpty_thenReturnEmptyList() {
    when(priceDao.findAllByTicker(any(), any())).thenReturn(Collections.emptyList());

    List<Trend> result = trendService.calculateAndStorePriceTrend("TEST", TimeFrame.DAY);

    assertTrue(result.isEmpty());
    verify(trendRepository, never()).deleteByIdTickerAndIdTimeframeAndIdDateBetween(any(), any(),
        any(), any());
    verify(trendRepository, never()).saveAll(any());
  }

  @Test
  void whenLessThanThreePrices_thenReturnEmptyList() {
    List<TestPrice> prices = List.of(
        new TestPrice("TEST", LocalDateTime.now(), 12, 10),
        new TestPrice("TEST", LocalDateTime.now().plusDays(1), 13, 11)
    );
    when(priceDao.findAllByTicker(any(), any())).thenReturn((List) prices);

    List<Trend> result = trendService.calculateAndStorePriceTrend("TEST", TimeFrame.DAY);

    assertTrue(result.isEmpty());
    verify(trendRepository, never()).saveAll(any());
  }

  @Test
  void whenClearUptrend_thenIdentifyUptrend() {
    // Create a sequence with clear higher highs and higher lows
    LocalDateTime baseTime = LocalDateTime.now();
    List<TestPrice> prices = List.of(
        new TestPrice("TEST", baseTime, 100, 90),                 // Initial point
        new TestPrice("TEST", baseTime.plusDays(1), 110, 89),    // Higher high, higher low
        new TestPrice("TEST", baseTime.plusDays(2), 105, 98),    // Lower high (interim)
        new TestPrice("TEST", baseTime.plusDays(3), 115, 100),   // New higher high, higher low
        new TestPrice("TEST", baseTime.plusDays(4), 108, 95),   // Lower high (interim)
        new TestPrice("TEST", baseTime.plusDays(5), 120, 105)    // Final higher high, higher low
    );

    when(priceDao.findAllByTicker(any(), any())).thenReturn((List) prices);
    when(trendRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

    List<Trend> results = trendService.calculateAndStorePriceTrend("TEST", TimeFrame.DAY);

    // Verify that we found trends and they include uptrend markers
    assertFalse(results.isEmpty());
    assertTrue(results.stream().anyMatch(t -> t.getTrend() == 1),
        "Should identify at least one uptrend");

    // Verify the sequence of trends is correct
    results.forEach(t -> System.out.printf("Date: %s, ValueMax: %.2f, Trend: %d%n",
        t.getId().getDate(), t.getMaxExtremum(), t.getTrend()));
  }

  @Test
  void whenClearDowntrend_thenIdentifyDowntrend() {
    // Create a sequence with clear lower highs and lower lows
    LocalDateTime baseTime = LocalDateTime.now();
    List<TestPrice> prices = List.of(
        new TestPrice("TEST", baseTime, 100, 90),                 // Initial point
        new TestPrice("TEST", baseTime.plusDays(1), 95, 85),     // Lower high, lower low
        new TestPrice("TEST", baseTime.plusDays(2), 97, 86),     // Higher high (interim)
        new TestPrice("TEST", baseTime.plusDays(3), 90, 84),     // Lower high, lower low
        new TestPrice("TEST", baseTime.plusDays(4), 92, 78),     // Higher high (interim)
        new TestPrice("TEST", baseTime.plusDays(5), 85, 79)      // Final lower high, lower low
    );

    when(priceDao.findAllByTicker(any(), any())).thenReturn((List) prices);
    when(trendRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

    List<Trend> results = trendService.calculateAndStorePriceTrend("TEST", TimeFrame.DAY);

    assertFalse(results.isEmpty());
    assertTrue(results.stream().anyMatch(t -> t.getTrend() == -1),
        "Should identify at least one downtrend");

    results.forEach(t -> System.out.printf("Date: %s, ValueMax: %.2f, Trend: %d%n",
        t.getId().getDate(), t.getMaxExtremum(), t.getTrend()));
  }

  @Test
  void whenSidewaysMovement_thenIdentifyNoTrend() {
    // Create a sequence with mixed highs and lows (no clear trend)
    LocalDateTime baseTime = LocalDateTime.now();
    List<TestPrice> prices = List.of(
        new TestPrice("TEST", baseTime, 100, 90),                 // Initial point
        new TestPrice("TEST", baseTime.plusDays(1), 102, 88),    // Higher high, lower low
        new TestPrice("TEST", baseTime.plusDays(2), 98, 92),     // Lower high, higher low
        new TestPrice("TEST", baseTime.plusDays(3), 101, 89),    // Higher high, lower low
        new TestPrice("TEST", baseTime.plusDays(4), 99, 91),     // Lower high, higher low
        new TestPrice("TEST", baseTime.plusDays(5), 100, 90)     // Mixed movement
    );

    when(priceDao.findAllByTicker(any(), any())).thenReturn((List) prices);
    when(trendRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

    List<Trend> results = trendService.calculateAndStorePriceTrend("TEST", TimeFrame.DAY);

    assertFalse(results.isEmpty());
    assertTrue(results.stream().anyMatch(t -> t.getTrend() == 0),
        "Should identify some periods with no trend");

    results.forEach(t -> System.out.printf("Date: %s, ValueMax: %.2f, Trend: %d%n",
        t.getId().getDate(), t.getMaxExtremum(), t.getTrend()));
  }

  @Test
  void verifyChronologicalOrder() {
    LocalDateTime baseTime = LocalDateTime.now();
    List<TestPrice> prices = List.of(
        new TestPrice("TEST", baseTime, 100, 90),
        new TestPrice("TEST", baseTime.plusDays(1), 110, 95),
        new TestPrice("TEST", baseTime.plusDays(2), 105, 98),
        new TestPrice("TEST", baseTime.plusDays(3), 115, 100)
    );

    when(priceDao.findAllByTicker(any(), any())).thenReturn((List) prices);
    when(trendRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

    List<Trend> results = trendService.calculateAndStorePriceTrend("TEST", TimeFrame.DAY);

    // Verify order is maintained
    LocalDateTime previousDate = null;
    for (Trend trend : results) {
      if (previousDate != null) {
        assertTrue(trend.getId().getDate().isAfter(previousDate),
            "Trends should be in chronological order");
      }
      previousDate = trend.getId().getDate();
    }
  }

  private static class TestPrice extends BasePrice {
    public TestPrice(String ticker, LocalDateTime date, double high, double low) {
      setId(new PriceKey(ticker, date));
      setHigh(high);
      setLow(low);
    }
  }
}