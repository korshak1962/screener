package korshak.com.screener.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceHour;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.SharePriceDownLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service("moexDownloader")
public class MoexSharePriceDownLoaderServiceImpl implements SharePriceDownLoaderService {
  private final String MOEX_BASE_URL = "https://iss.moex.com/iss/engines/stock/markets/shares/securities/";
  private final PriceDao priceDao;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Autowired
  public MoexSharePriceDownLoaderServiceImpl(PriceDao priceDao, RestTemplate restTemplate) {
    this.priceDao = priceDao;
    this.restTemplate = restTemplate;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public int fetchAndSaveData(String timeSeriesLabel, String ticker, String interval, String yearMonth) {
    return fetchAndSaveData(ticker, yearMonth);
  }

  @Override
  public int fetchAndSaveData(String ticker, String yearMonth) {
    try {
      String[] parts = yearMonth.split("-");
      int year = Integer.parseInt(parts[0]);
      int month = Integer.parseInt(parts[1]);

      LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
      LocalDateTime endDate = startDate.plusDays(10);

      // Check if data exists
      List<? extends BasePrice> existingData = priceDao.findByDateRange(
          ticker,
          startDate,
          endDate,
          TimeFrame.HOUR
      );

      if (!existingData.isEmpty()) {
        System.out.println("Data for ticker " + ticker + " and month " + yearMonth + " already exists in DB");
        return 0;
      }

      String url = buildMoexUrl(ticker, yearMonth);
      System.out.println("Requesting URL: " + url);

      String response = restTemplate.getForObject(url, String.class);
      if (response == null) {
        throw new RuntimeException("No response from MOEX API");
      }

      JsonNode root = parseResponse(response);
      List<PriceHour> priceDataList = extractPriceData(root, ticker+"_MOEX");

      if (priceDataList.isEmpty()) {
        System.out.println("No data found for " + ticker + " in " + yearMonth);
        return 0;
      }

      System.out.println("Saving " + priceDataList.size() + " records for " + ticker);
      List<PriceHour> saved = priceDao.saveAll(priceDataList);
      return saved.size();

    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid yearMonth format. Expected YYYY-MM, got: " + yearMonth, e);
    }
  }

  private String buildMoexUrl(String ticker, String yearMonth) {
    String[] parts = yearMonth.split("-");
    String fromDate = String.format("%s-%s-01", parts[0], parts[1]);

    // Check if this is the current month
    YearMonth requestedMonth = YearMonth.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    YearMonth currentMonth = YearMonth.now();

    String baseUrl = MOEX_BASE_URL + ticker + "/candles.json?iss.meta=off&interval=60";

    if (requestedMonth.equals(currentMonth)) {
      // For current month, only use fromDate
      return baseUrl + "&from=" + fromDate;
    } else {
      // For past months, use both from and till dates
      String tillDate = String.format("%s-%s-31", parts[0], parts[1]);
      return baseUrl + "&from=" + fromDate + "&till=" + tillDate;
    }
  }

  private JsonNode parseResponse(String response) {
    try {
      return objectMapper.readTree(response);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse MOEX response", e);
    }
  }

  public String getDbTicker() {
    return dbTicker;
  }

  String dbTicker;
  private List<PriceHour> extractPriceData(JsonNode root, String ticker) {
    dbTicker = ticker;
    List<PriceHour> priceDataList = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    JsonNode candles = root.path("candles");
    JsonNode columnsNode = candles.path("columns");
    JsonNode dataNode = candles.path("data");

    int openIdx = getColumnIndex(columnsNode, "open");
    int closeIdx = getColumnIndex(columnsNode, "close");
    int highIdx = getColumnIndex(columnsNode, "high");
    int lowIdx = getColumnIndex(columnsNode, "low");
    int volumeIdx = getColumnIndex(columnsNode, "volume");
    int beginIdx = getColumnIndex(columnsNode, "begin");

    for (JsonNode candle : dataNode) {
      try {
        LocalDateTime dateTime = LocalDateTime.parse(candle.get(beginIdx).asText(), formatter);
        double open = candle.get(openIdx).asDouble();
        double close = candle.get(closeIdx).asDouble();
        double high = candle.get(highIdx).asDouble();
        double low = candle.get(lowIdx).asDouble();
        long volume = candle.get(volumeIdx).asLong();

        PriceHour priceHour = new PriceHour();
        priceHour.setId(new PriceKey(ticker, dateTime));
        priceHour.setOpen(open);
        priceHour.setHigh(high);
        priceHour.setLow(low);
        priceHour.setClose(close);
        priceHour.setVolume(volume);

        priceDataList.add(priceHour);
      } catch (Exception e) {
        System.err.println("Error processing candle: " + candle);
        throw e;
      }
    }

    return priceDataList;
  }

  private int getColumnIndex(JsonNode columnsNode, String columnName) {
    for (int i = 0; i < columnsNode.size(); i++) {
      if (columnsNode.get(i).asText().equals(columnName)) {
        return i;
      }
    }
    throw new RuntimeException("Column " + columnName + " not found in MOEX response");
  }
}