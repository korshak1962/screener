package korshak.com.screener.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceMin5;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.SharePriceDownLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service("AlphaVintageDownloader")
public class AlphaVintageDownloader implements SharePriceDownLoaderService {
  String apiKey;
  String baseUrl;
  PriceDao priceDao;
  String dbTicker;
  @Autowired
  private RestTemplate restTemplate;


  public AlphaVintageDownloader(PriceDao priceDao,
                                @Value("${alpha.apiKey}") String apiKey,
                                @Value("${alpha.baseUrl}") String baseUrl) {
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
    this.priceDao = priceDao;
  }

  /*
      @Override
      public SharePrice updateSharePrice(SharePrice sharePrice) {
          return sharePriceRepository.save(sharePrice);
      }

      @Override
      public void deleteSharePrice(String ticker, LocalDateTime date) {
          sharePriceRepository.deleteById(new SharePriceId(ticker, date));
      }
  */
  //json
  public int fetchAndSaveData(String timeSeriesLabel, String ticker, String interval,
                              String yearMonth) {
    dbTicker = ticker;
    String url =
        baseUrl + timeSeriesLabel + "&symbol=" + ticker + "&interval=" + interval + "&month=" +
            yearMonth
            + "&outputsize=full&apikey=" + apiKey;
    ResponseEntity<String> response = restTemplate.getForEntity(
        url, String.class);
    System.out.println("response code = " + response.getStatusCode());
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode root = null;
    try {
      root = objectMapper.readTree(response.getBody());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    JsonNode timeSeries = root.path("Time Series (5min)");
    System.out.println("response size = " + timeSeries.size());
    List<PriceMin5> intradayDataList = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    Iterator<Map.Entry<String, JsonNode>> entrys = timeSeries.fields();
    LocalDateTime localDateTime = null;
    while (entrys.hasNext()) {
      Map.Entry<String, JsonNode> dataNode = entrys.next();
      localDateTime = LocalDateTime.parse(dataNode.getKey(), formatter);
      double open = Double.parseDouble(dataNode.getValue().path("1. open").asText());
      double high = Double.parseDouble(dataNode.getValue().path("2. high").asText());
      double low = Double.parseDouble(dataNode.getValue().path("3. low").asText());
      double close = Double.parseDouble(dataNode.getValue().path("4. close").asText());
      long volume = Long.parseLong(dataNode.getValue().path("5. volume").asText());
      PriceMin5 priceMin5 = new PriceMin5(
          new PriceKey(ticker, localDateTime), open, high, low, close, volume);
      intradayDataList.add(priceMin5);
    }
    System.out.println("to be saved = " + intradayDataList.size() + " for date: " + localDateTime);
    List<PriceMin5> saved = priceDao.saveAll(intradayDataList);
    return saved.size();
  }

  @Override
  public int fetchAndSaveData(String ticker, String yearMonth) {
    try {
      // Parse year and month from yearMonth string (format: "2024-01")
      String[] parts = yearMonth.split("-");
      int year = Integer.parseInt(parts[0]);
      int month = Integer.parseInt(parts[1]);

      // Create date range for first 10 days of the month
      LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
      LocalDateTime endDate = startDate.plusDays(10);

      // Check if data exists by looking for any records in first 10 days
      List<? extends BasePrice> existingData = priceDao.findByDateRange(
          ticker,
          startDate,
          endDate,
          TimeFrame.DAY
      );

      if (!existingData.isEmpty()) {
        System.out.println(
            "Data for ticker " + ticker + " and month " + yearMonth + " already exists in DB");
        return 0;
      }

      // If data doesn't exist, proceed with fetching and saving
      String interval = "5min";
      final String timeSeriesLabel = "TIME_SERIES_INTRADAY";
      return fetchAndSaveData(timeSeriesLabel, ticker, interval, yearMonth);

    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid yearMonth format. Expected YYYY-MM, got: " + yearMonth, e);
    }
  }

  @Override
  public int fetchAndSaveDataFromDate(String ticker, LocalDate startDate) {
    return 0;
  }

  @Override
  public String getDbTicker() {
    return dbTicker;
  }
}
