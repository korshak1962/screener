package korshak.com.screener;

import korshak.com.screener.service.download.SharePriceDownLoaderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ScreenerApplicationTests {
  //    //https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=IBM&apikey=demo
  @Autowired
  private RestTemplate restTemplate;
  @Autowired
  private SharePriceDownLoaderService sharePriceDownLoaderService;

  @Test
  void test() {
    System.out.println("test");
  }

}
