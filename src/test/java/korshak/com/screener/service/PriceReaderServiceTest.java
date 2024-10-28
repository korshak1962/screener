package korshak.com.screener.service;

import korshak.com.screener.dao.PriceMin5;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.Month;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class PriceReaderServiceTest {
    @Autowired
    private SharePriceDownLoaderService sharePriceDownLoaderService;
    @Autowired
    private PriceReaderService priceReaderService;

    @Test
    void testSaveAndRead() {
        final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
        final String ticker = "IBM";
        String interval = "5min";
        String month = "2009-01";

        sharePriceDownLoaderService.fetchAndSaveData(timeSeriesLabel, ticker, interval, month);

        LocalDateTime start = LocalDateTime.of(2009, Month.JANUARY, 4, 0, 0);
        LocalDateTime end = LocalDateTime.of(2009, Month.JANUARY, 8, 0, 0);
        Page<PriceMin5> prices = priceReaderService.getSharePricesBetweenDates(ticker, start, end, Pageable.unpaged());
        System.out.println("==========prices.getTotalElements() = " + prices.getTotalElements());
        PriceMin5 first =prices.stream().findFirst().get();
        System.out.println(" date = "+first.getId().getDate() + first.getClose());
    }
}
