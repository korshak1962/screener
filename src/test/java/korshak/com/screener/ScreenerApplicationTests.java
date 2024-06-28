package korshak.com.screener;

import korshak.com.screener.dao.SharePrice;
import korshak.com.screener.service.PriceReaderService;
import korshak.com.screener.service.SharePriceDownLoaderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.Month;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ScreenerApplicationTests {
//    //https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=IBM&apikey=demo
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private SharePriceDownLoaderService sharePriceDownLoaderService;
	@Autowired
	private PriceReaderService priceReaderService;
	@Test
	void test() {
System.out.println("test");
	}

}
