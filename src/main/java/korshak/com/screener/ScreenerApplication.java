package korshak.com.screener;

import korshak.com.screener.service.SharePriceDownLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScreenerApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ScreenerApplication.class, args);
    }

    @Autowired
    @Qualifier("json")
    private SharePriceDownLoaderService sharePriceDownLoaderService;

    @Override
    public void run(String... args) throws Exception {
    //    downloadSeries();
    }

    private void downloadSeries() {
        final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
        final String ticker = "SPY";
        String interval = "5min";
        String month = "2024-03";
        sharePriceDownLoaderService.fetchAndSaveData(timeSeriesLabel, ticker, interval, month);
        System.exit(0);
    }
}
