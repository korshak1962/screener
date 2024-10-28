package korshak.com.screener.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class PriceMin5RepositoryTest {
    @Autowired
    private PriceMin5Repository priceMin5Repository;

    @Test
    public void testSharePrice() {
        // Create a new IntradayData object
        LocalDateTime now = LocalDateTime.of(2024, Month.JANUARY,10,10,15);
        double open = 100.50;
        double high = 101.25;
        double low = 99.75;
        double close = 100.00;
        long volume = 10000;
        String ticker = "testTicker";
        PriceMin5
            priceMin5 = new PriceMin5(new PriceKey(ticker, now.minus(Duration.ofDays(1))), open - 1, high - 1, low - 1, close - 1, volume - 1);
        priceMin5Repository.save(priceMin5);
        priceMin5 = new PriceMin5(new PriceKey(ticker, now), open, high, low, close, volume);
        priceMin5Repository.save(priceMin5);

        // Fetch the saved priceMin5 from the database
        Optional<PriceMin5> savedData =
                priceMin5Repository.findById(new PriceKey(priceMin5.getId().getTicker(), priceMin5.getId().getDate()));
        assertFalse(savedData.isEmpty());
        PriceMin5 retrievedSharePrice = savedData.get();
        assertEquals(priceMin5.getId().getDate(), retrievedSharePrice.getId().getDate());
        assertEquals(priceMin5.getOpen(), retrievedSharePrice.getOpen(), 0.001); // Delta for double comparison
        assertEquals(high, retrievedSharePrice.getHigh(), 0.001);
        assertEquals(low, retrievedSharePrice.getLow(), 0.001);
        assertEquals(close, retrievedSharePrice.getClose(), 0.001);
        assertEquals(volume, retrievedSharePrice.getVolume());

        Page<PriceMin5> pagedSavedData = priceMin5Repository.findById_TickerAndId_DateBetween(priceMin5.getId().getTicker(), now.minus(Duration.ofDays(2)), now, Pageable.unpaged());
        assertFalse(pagedSavedData.isEmpty());
        assertEquals(2, pagedSavedData.getTotalElements());
        pagedSavedData = priceMin5Repository.findById_TickerAndId_DateBetween(priceMin5.getId().getTicker(), now.minus(Duration.ofDays(1)), now.minus(Duration.ofDays(1)), Pageable.unpaged());
        assertFalse(pagedSavedData.isEmpty());
        assertEquals(1, pagedSavedData.getTotalElements());
    }
}
