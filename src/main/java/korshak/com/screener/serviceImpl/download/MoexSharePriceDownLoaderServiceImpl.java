package korshak.com.screener.serviceImpl.download;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceHour;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.download.SharePriceDownLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service("moexDownloader")
public class MoexSharePriceDownLoaderServiceImpl implements SharePriceDownLoaderService {
  /**
   * Downloads and saves data from a given start date up to today
   *
   * @param ticker The ticker symbol
   * @param startDate Start date in format "YYYY-MM-DD"
   * @return Number of records saved
   */
  private static final int MAX_RECORDS = 500;
  // Assuming trading hours 10:00-18:45, that's about 9 hours = 9 records per day
  // So 500/9 ≈ 55 days would be safe for one request
  private static final int DAYS_PER_CHUNK = 30;
  private final String MOEX_BASE_URL =
      "https://iss.moex.com/iss/engines/stock/markets/shares/securities/";
  private final PriceDao priceDao;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  String dbTicker;

  @Autowired
  public MoexSharePriceDownLoaderServiceImpl(PriceDao priceDao, RestTemplate restTemplate) {
    this.priceDao = priceDao;
    this.restTemplate = restTemplate;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public int fetchAndSaveData(String timeSeriesLabel, String ticker, String interval,
                              int year,int month) {
    return fetchAndSaveData(ticker, year,month);
  }

  @Override
  public int fetchAndSaveData(String ticker, int year,int month) {
    try {
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
        System.out.println(
            "Data for ticker " + ticker +" year  "+year+ " and month " + month + " already exists in DB");
        return 0;
      }

      String url = buildMoexUrl(ticker, year,month);
      System.out.println("Requesting URL: " + url);

      String response = restTemplate.getForObject(url, String.class);
      if (response == null) {
        throw new RuntimeException("No response from MOEX API");
      }

      JsonNode root = parseResponse(response);
      dbTicker = ticker + "_MOEX";
      List<PriceHour> priceDataList = extractPriceData(root, dbTicker);

      if (priceDataList.isEmpty()) {
        System.out.println("No data found for " + ticker + " in " + year+" "+month);
        return 0;
      }

      System.out.println("Saving " + priceDataList.size() + " records for " + ticker);
      List<PriceHour> saved = priceDao.saveAll(priceDataList);
      return saved.size();

    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid yearMonth format. Expected YYYY-MM, got: " + year+" "+month, e);
    }
  }

  private String buildMoexUrl(String ticker, int year,int month) {

    YearMonth requestedYearMonth = YearMonth.of(year, month);
    YearMonth currentYearMonth = YearMonth.now();

    // Get first day of month
    String fromDate = String.format("%s-%02d-01", year, month);

    // Build base URL
    String baseUrl = MOEX_BASE_URL + ticker + "/candles.json?iss.meta=off&interval=60";

    if (requestedYearMonth.equals(currentYearMonth)) {
      // For current month, only use fromDate
      return baseUrl + "&from=" + fromDate;
    } else {
      // For past months, get the actual last day of the month
      int lastDay = requestedYearMonth.lengthOfMonth();
      String tillDate = String.format("%s-%02d-%02d", year, month, lastDay);
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

  @Override
  public int downloadFromDateUpToday(String ticker, LocalDate startDate) {
    throw new UnsupportedOperationException("MOEX does not support this operation");
   // return 0;
    //return downloadForExactDates(ticker, startDate, LocalDate.now());
  }

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

  public int fetchAndSaveDataFromDate(String ticker, LocalDate startDate) {
    LocalDate currentDate = LocalDate.now();
    LocalDate chunkStart = startDate;
    int totalSaved = 0;

    while (chunkStart.isBefore(currentDate)) {
      LocalDate chunkEnd = chunkStart.plusDays(DAYS_PER_CHUNK);
      if (chunkEnd.isAfter(currentDate)) {
        chunkEnd = currentDate;
      }

      String url = buildChunkedUrl(ticker, chunkStart, chunkEnd);
      System.out.println("Requesting chunk from " + chunkStart + " to " + chunkEnd);
      System.out.println("URL: " + url);

      try {
        String response = restTemplate.getForObject(url, String.class);
        if (response == null) {
          throw new RuntimeException("No response from MOEX API");
        }

        JsonNode root = parseResponse(response);
        List<PriceHour> priceDataList = extractPriceData(root, ticker + "_MOEX");

        if (!priceDataList.isEmpty()) {
          System.out.println("Saving " + priceDataList.size() + " records for chunk");
          List<PriceHour> saved = priceDao.saveAll(priceDataList);
          totalSaved += saved.size();
        }

        // Add a small delay between requests to be nice to the API
        Thread.sleep(100);
      } catch (Exception e) {
        System.err.println(
            "Error processing chunk " + chunkStart + " to " + chunkEnd + ": " + e.getMessage());
        // Continue with next chunk despite error
      }

      chunkStart = chunkEnd.plusDays(1);
    }

    System.out.println("Total records saved: " + totalSaved);
    return totalSaved;
  }

  public int fetchAndSaveDataFromDate(String ticker, String startDate) {
    LocalDate parsedDate = LocalDate.parse(startDate);
    return fetchAndSaveDataFromDate(ticker, parsedDate);
  }

  private String buildChunkedUrl(String ticker, LocalDate fromDate, LocalDate tillDate) {
    return MOEX_BASE_URL + ticker +
        "/candles.json" +
        "?iss.meta=off" +
        "&interval=60" +
        "&from=" + fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE) +
        "&till=" + tillDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }
}