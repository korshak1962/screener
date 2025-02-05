package korshak.com.screener.serviceImpl.strategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceMin5;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BaseStrategyTest {

  @Mock
  private PriceDao priceDao;

  private TestStrategy strategy;
  private final String TEST_TICKER = "TEST";
  private final LocalDateTime START_DATE = LocalDateTime.of(2024, 1, 1, 0, 0);
  private final LocalDateTime END_DATE = LocalDateTime.of(2024, 1, 2, 0, 0);

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    strategy = new TestStrategy(priceDao);
    strategy.init(TEST_TICKER, TimeFrame.HOUR, START_DATE, END_DATE);
  }

  @Test
  void getAllSignals_WithHigherTimeframe_ThrowsException() {
    assertThrows(RuntimeException.class, () ->
        strategy.getAllSignals(TimeFrame.DAY)
    );
  }

  @Test
  void getAllSignals_WithSameTimeframe_ReturnsBaseSignals() {
    // Setup base signals
    Signal signal = new Signal(START_DATE, 100.0, SignalType.LongOpen);
    strategy.allSignals = List.of(signal);

    List<Signal> result = strategy.getAllSignals(TimeFrame.HOUR);

    assertEquals(1, result.size());
    assertEquals(signal, result.get(0));
  }

  @Test
  void getAllSignals_WithLowerTimeframe_ProcessesSignalsCorrectly() {
    // Create a spy of the strategy to mock getPricesForTimeframe
    TestStrategy strategySpy = spy(strategy);

    // Setup test data for increasing prices
    LocalDateTime time1 = START_DATE;
    LocalDateTime time2 = START_DATE.plusMinutes(5);
    LocalDateTime time3 = START_DATE.plusMinutes(10);

    // Create base signals for HOUR timeframe (needed for initialization)
    Signal hourSignal = new Signal(time1, 100.0, SignalType.LongOpen);
    strategySpy.allSignals = List.of(hourSignal);

    // Create 5-minute prices with clear price increases
    List<BasePrice> min5Prices = new ArrayList<>();
    min5Prices.add(createPrice(time1, 100.0, 102.0, 99.0, 100.0, 1000));  // Closes at 100
    min5Prices.add(createPrice(time2, 100.0, 106.0, 100.0, 105.0, 1200)); // Closes at 105 (increase)
    min5Prices.add(createPrice(time3, 105.0, 110.0, 104.0, 108.0, 1100)); // Closes at 108 (increase)

    System.out.println("Test prices created:");
    for (BasePrice price : min5Prices) {
      System.out.println(price.getId().getDate() + ": " + price.getClose());
    }

    // Mock getPricesForTimeframe behavior
    doReturn(min5Prices).when(strategySpy).getPricesForTimeframe(TimeFrame.MIN5);

    // Execute test
    List<Signal> result = strategySpy.getAllSignals(TimeFrame.MIN5);

    // Verify results
    assertNotNull(result);
    assertEquals(2, result.size(), "Should generate signals for two price increases");

    // Verify first signal (100 -> 105)
    assertEquals(time2, result.get(0).getDate(), "First signal should be at second price point");
    assertEquals(105.0, result.get(0).getPrice(), "First signal should be at price 105");
    assertEquals(SignalType.LongOpen, result.get(0).getSignalType());

    // Verify second signal (105 -> 108)
    assertEquals(time3, result.get(1).getDate(), "Second signal should be at third price point");
    assertEquals(108.0, result.get(1).getPrice(), "Second signal should be at price 108");
    assertEquals(SignalType.LongOpen, result.get(1).getSignalType());
  }

  @Test
  void getAllSignals_WithNoBaseSignals_ReturnsEmptyList() {
    strategy.allSignals = new ArrayList<>();

    List<Signal> result = strategy.getAllSignals(TimeFrame.MIN5);

    assertTrue(result.isEmpty());
  }

  @Test
  void getAllSignals_WithNoPrices_ReturnsEmptyList() {
    // Setup a base signal
    strategy.allSignals = List.of(new Signal(START_DATE, 100.0, SignalType.LongOpen));

    // Mock empty price list
    when(priceDao.findByDateRange(any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());

    List<Signal> result = strategy.getAllSignals(TimeFrame.MIN5);

    assertTrue(result.isEmpty());
  }

  private PriceMin5 createPrice(LocalDateTime dateTime, double open, double high, double low, double close, long volume) {
    return new PriceMin5(
        new PriceKey(TEST_TICKER, dateTime),
        open, high, low, close, volume
    );
  }

  // Test implementation of BaseStrategy
  private static class TestStrategy extends BaseStrategy {
    public List<Signal> allSignals = new ArrayList<>();

    public TestStrategy(PriceDao priceDao) {
      super(priceDao);
    }

    @Override
    public Signal getSignal(BasePrice price) {
      // Simple implementation for testing
      return new Signal(price.getId().getDate(), price.getClose(), SignalType.LongOpen);
    }


    private BasePrice lastProcessedPrice = null;

    @Override
    public Signal getSignal(BasePrice prevPrice, BasePrice price) {
      System.out.println("getSignal comparing prices: " +
          prevPrice.getClose() + " -> " + price.getClose());

      if (price.getClose() > prevPrice.getClose()) {
        Signal signal = new Signal(price.getId().getDate(), price.getClose(), SignalType.LongOpen);
        System.out.println("*** Generated signal at " + signal.getDate() +
            " price: " + signal.getPrice() +
            " due to increase from " + prevPrice.getClose());
        return signal;
      }
      System.out.println("No signal generated (no price increase)");
      return null;
    }

    @Override
    public List<Signal> getAllSignals() {
      return allSignals;
    }
  }
}