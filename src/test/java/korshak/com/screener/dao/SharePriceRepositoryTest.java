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
public class SharePriceRepositoryTest {
    @Autowired
    private SharePriceRepository sharePriceRepository;

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
        SharePrice sharePrice = new SharePrice(ticker, now.minus(Duration.ofDays(1)), open - 1, high - 1, low - 1, close - 1, volume - 1);
        sharePriceRepository.save(sharePrice);
        sharePrice = new SharePrice(ticker, now, open, high, low, close, volume);
        sharePriceRepository.save(sharePrice);

        // Fetch the saved sharePrice from the database
        Optional<SharePrice> savedData =
                sharePriceRepository.findById(new SharePriceId(sharePrice.getTicker(), sharePrice.getDate()));
        assertFalse(savedData.isEmpty());
        SharePrice retrievedSharePrice = savedData.get();
        assertEquals(sharePrice.getDate(), retrievedSharePrice.getDate());
        assertEquals(sharePrice.getOpen(), retrievedSharePrice.getOpen(), 0.001); // Delta for double comparison
        assertEquals(high, retrievedSharePrice.getHigh(), 0.001);
        assertEquals(low, retrievedSharePrice.getLow(), 0.001);
        assertEquals(close, retrievedSharePrice.getClose(), 0.001);
        assertEquals(volume, retrievedSharePrice.getVolume());

        Page<SharePrice> pagedSavedData = sharePriceRepository.findByTickerAndDateBetween(sharePrice.getTicker(), now.minus(Duration.ofDays(2)), now, Pageable.unpaged());
        assertFalse(pagedSavedData.isEmpty());
        assertEquals(2, pagedSavedData.getTotalElements());
        pagedSavedData = sharePriceRepository.findByTickerAndDateBetween(sharePrice.getTicker(), now.minus(Duration.ofDays(1)), now.minus(Duration.ofDays(1)), Pageable.unpaged());
        assertFalse(pagedSavedData.isEmpty());
        assertEquals(1, pagedSavedData.getTotalElements());
    }
}
