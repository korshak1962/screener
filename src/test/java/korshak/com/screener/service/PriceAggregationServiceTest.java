package korshak.com.screener.service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import korshak.com.screener.dao.PriceDay;
import korshak.com.screener.dao.PriceDayRepository;
import korshak.com.screener.dao.PriceHour;
import korshak.com.screener.dao.PriceHourRepository;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceMin5;
import korshak.com.screener.dao.PriceMin5Repository;
import korshak.com.screener.dao.PriceMonthRepository;
import korshak.com.screener.dao.PriceWeekRepository;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.serviceImpl.PriceAggregationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class PriceAggregationServiceTest {

  @Mock
  private PriceMin5Repository priceMin5Repository;
  @Mock
  private PriceHourRepository priceHourRepository;
  @Mock
  private PriceDayRepository priceDayRepository;
  @Mock
  private PriceWeekRepository priceWeekRepository;
  @Mock
  private PriceMonthRepository priceMonthRepository;

  private PriceAggregationService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new PriceAggregationServiceImpl(
        priceMin5Repository,
        priceHourRepository,
        priceDayRepository,
        priceWeekRepository,
        priceMonthRepository
    );
  }

  @Test
  void testAggregateToHourly() {
    // Given
    String ticker = "AAPL";
    LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 10, 0);
    List<PriceMin5> min5Data = Arrays.asList(
        createPriceMin5(ticker, baseTime, 100, 105, 99, 101, 1000),
        createPriceMin5(ticker, baseTime.plusMinutes(5), 101, 106, 100, 102, 1100),
        createPriceMin5(ticker, baseTime.plusMinutes(10), 102, 107, 101, 103, 900)
    );

    when(priceMin5Repository.findById_Ticker(ticker)).thenReturn(min5Data);

    // When
    service.aggregateData(ticker, TimeFrame.HOUR);

    // Then
    verify(priceHourRepository).saveAll(argThat(hourPrices -> {
      List<PriceHour> priceList = new ArrayList<>();
      hourPrices.forEach(priceList::add);

      if (priceList.isEmpty()) return false;

      PriceHour price = priceList.get(0);
      return price.getOpen() == 100 &&
          price.getHigh() == 107 &&
          price.getLow() == 99 &&
          price.getClose() == 103 &&
          price.getVolume() == 3000 &&
          price.getId().getTicker().equals(ticker) &&
          price.getId().getDate().equals(baseTime.truncatedTo(ChronoUnit.HOURS));
    }));
  }

  @Test
  void testAggregateToDaily() {
    // Given
    String ticker = "AAPL";
    LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 10, 0);
    List<PriceMin5> min5Data = Arrays.asList(
        createPriceMin5(ticker, baseTime, 100, 105, 99, 101, 1000),
        createPriceMin5(ticker, baseTime.plusHours(2), 101, 106, 100, 102, 1100),
        createPriceMin5(ticker, baseTime.plusHours(4), 102, 107, 101, 103, 900)
    );

    when(priceMin5Repository.findById_Ticker(ticker)).thenReturn(min5Data);

    // When
    service.aggregateData(ticker, TimeFrame.DAY);

    // Then
    verify(priceDayRepository).saveAll(argThat(dayPrices -> {
      List<PriceDay> priceList = new ArrayList<>();
      dayPrices.forEach(priceList::add);

      if (priceList.isEmpty()) return false;

      PriceDay price = priceList.get(0);
      return price.getOpen() == 100 &&
          price.getHigh() == 107 &&
          price.getLow() == 99 &&
          price.getClose() == 103 &&
          price.getVolume() == 3000 &&
          price.getId().getTicker().equals(ticker) &&
          price.getId().getDate().equals(baseTime.truncatedTo(ChronoUnit.DAYS));
    }));
  }

  @Test
  void testMultipleDaysAggregation() {
    // Given
    String ticker = "AAPL";
    LocalDateTime day1 = LocalDateTime.of(2024, 1, 1, 10, 0);
    LocalDateTime day2 = LocalDateTime.of(2024, 1, 2, 10, 0);

    List<PriceMin5> min5Data = Arrays.asList(
        createPriceMin5(ticker, day1, 100, 105, 99, 101, 1000),
        createPriceMin5(ticker, day1.plusHours(2), 101, 106, 100, 102, 1100),
        createPriceMin5(ticker, day2, 102, 107, 101, 103, 900),
        createPriceMin5(ticker, day2.plusHours(2), 103, 108, 102, 104, 800)
    );

    when(priceMin5Repository.findById_Ticker(ticker)).thenReturn(min5Data);

    // When
    service.aggregateData(ticker, TimeFrame.DAY);

    // Then
    verify(priceDayRepository).saveAll(argThat(dayPrices -> {
      List<PriceDay> priceList = new ArrayList<>();
      dayPrices.forEach(priceList::add);

      return priceList.size() == 2 &&
          priceList.get(0).getId().getDate().equals(day1.truncatedTo(ChronoUnit.DAYS)) &&
          priceList.get(1).getId().getDate().equals(day2.truncatedTo(ChronoUnit.DAYS));
    }));
  }

  @Test
  void testEmptyDataHandling() {
    // Given
    String ticker = "AAPL";
    when(priceMin5Repository.findById_Ticker(ticker)).thenReturn(List.of());

    // When
    service.aggregateData(ticker, TimeFrame.DAY);

    // Then
    verify(priceDayRepository, never()).saveAll(anyList());
  }

  private PriceMin5 createPriceMin5(String ticker, LocalDateTime dateTime,
                                    double open, double high, double low, double close, long volume) {
    PriceMin5 price = new PriceMin5();
    PriceKey key = new PriceKey();
    key.setTicker(ticker);
    key.setDate(dateTime);
    price.setId(key);
    price.setOpen(open);
    price.setHigh(high);
    price.setLow(low);
    price.setClose(close);
    price.setVolume(volume);
    return price;
  }
}