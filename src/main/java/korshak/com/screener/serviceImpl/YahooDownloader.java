package korshak.com.screener.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.PriceDao;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceMin5;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.SharePriceDownLoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service("yahooDownloader")
public class YahooDownloader implements SharePriceDownLoaderService {

  private static final Logger logger = LoggerFactory.getLogger(YahooDownloader.class);
  private static final String YAHOO_BASE_URL = "https://query2.finance.yahoo.com/v8/finance/chart/";
  private static final int DAYS_PER_CHUNK = 5; // Reduced from 7 to be more conservative
  private static final int MAX_RETRIES = 5; // Increased from 3
  private static final long INITIAL_RETRY_DELAY_MS = 10000; // 10 seconds initial delay
  private static final int MAX_CONSECUTIVE_FAILURES = 5; // Increased from 3
  private static final String[] USER_AGENTS = {
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0"
  };

  // Add market hours constants to match PriceAggregationServiceImpl
  private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);  // ET
  private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0); // ET

  // Add explicit ZoneId constants for proper time zone handling
  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final ZoneId EASTERN = ZoneId.of("America/New_York");

  private final PriceDao priceDao;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final Random random;
  private String dbTicker;

  @Autowired
  public YahooDownloader(PriceDao priceDao, RestTemplate restTemplate) {
    this.priceDao = priceDao;
    this.restTemplate = restTemplate;
    this.objectMapper = new ObjectMapper();
    this.random = new Random();
  }

  @jakarta.annotation.PostConstruct
  public void logTimeZoneInfo() {
    logger.info("YahooDownloader initialized with FORCED timezone correction");
    logger.info("We're now explicitly constructing times from components and adding post-save verification");
    logger.info("Market hours filter: {} to {}", MARKET_OPEN, MARKET_CLOSE);

    // Create a sample time to demonstrate what we're getting vs storing
    LocalDateTime utcSample = LocalDateTime.of(2024, 1, 10, 14, 30);
    LocalDateTime etSample = utcSample.minusHours(5);

    logger.info("Example conversion: {} UTC -> {} ET", utcSample, etSample);
    logger.info("Times in database should now appear as: {}", etSample);
  }

  private LocalDateTime createDateTimeFromTimestamp(long timestamp) {
    // First create the UTC datetime for this timestamp
    LocalDateTime utcDateTime = LocalDateTime.ofEpochSecond(
        timestamp,
        0,
        ZoneOffset.UTC
    );

    // Convert to Eastern Time by subtracting 5 hours
    return utcDateTime.minusHours(5);
  }

  @Override
  public int fetchAndSaveData(String ticker, String yearMonth) {
    try {
      String[] parts = yearMonth.split("-");
      int year = Integer.parseInt(parts[0]);
      int month = Integer.parseInt(parts[1]);

      LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
      LocalDateTime endDate = startDate.plusDays(10);

      // Check for existing data
      List<? extends BasePrice> existingData = priceDao.findByDateRange(
          ticker,
          startDate,
          endDate,
          TimeFrame.MIN5
      );

      if (!existingData.isEmpty()) {
        logger.info("Data for ticker {} and month {} already exists in DB", ticker, yearMonth);
        return 0;
      }

      dbTicker = ticker;
      return downloadDataInChunks(ticker, YearMonth.of(year, month));

    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid yearMonth format. Expected YYYY-MM, got: " + yearMonth, e);
    }
  }

  private int downloadDataInChunks(String ticker, YearMonth yearMonth) {
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    int totalSaved = 0;
    int consecutiveFailures = 0;

    // Initial delay before starting downloads
    randomDelay(2000, 5000);

    while (startDate.isBefore(endDate) || startDate.equals(endDate)) {
      LocalDate chunkEnd = startDate.plusDays(DAYS_PER_CHUNK - 1);
      if (chunkEnd.isAfter(endDate)) {
        chunkEnd = endDate;
      }

      try {
        logger.info("Downloading chunk for {} from {} to {}", ticker, startDate, chunkEnd);
        List<PriceMin5> priceData = downloadChunk(ticker, startDate, chunkEnd);

        if (!priceData.isEmpty()) {
          logger.info("Retrieved {} records for {}", priceData.size(), ticker);
          List<PriceMin5> saved = priceDao.saveAll(priceData);
          totalSaved += saved.size();
          consecutiveFailures = 0; // Reset on success

          // Random delay between successful chunks
          randomDelay(3000, 8000);
        } else {
          logger.warn("No data retrieved for {} between {} and {}",
              ticker, startDate, chunkEnd);
          consecutiveFailures++;
        }

      } catch (Exception e) {
        // Check if this is a "Data doesn't exist" error, which is expected for new securities
        if (e.getMessage() != null && e.getMessage().contains("Data doesn't exist")) {
          logger.info("No data available for {} from {} to {} (security may not have been trading yet)",
              ticker, startDate, chunkEnd);
          // This is not a failure, just no data for this period - continue with next chunk
        } else {
          // This is an actual error
          logger.error("Error downloading chunk {} to {} for {}: {}",
              startDate, chunkEnd, ticker, e.getMessage());

          consecutiveFailures++;
          if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            logger.error("Aborting download for {} after {} consecutive failures",
                ticker, MAX_CONSECUTIVE_FAILURES);
            throw new RuntimeException("Download aborted due to too many failures: " +
                e.getMessage());
          }

          // Longer delay after failure
          long delayMs = INITIAL_RETRY_DELAY_MS * (long)Math.pow(2, consecutiveFailures - 1);
          randomDelay(delayMs, delayMs + 5000);
        }
      }

      startDate = chunkEnd.plusDays(1);
    }

    logger.info("Successfully saved {} records for {}", totalSaved, ticker);
    return totalSaved;
  }

  private List<PriceMin5> downloadChunk(String ticker, LocalDate startDate, LocalDate endDate) {
    String url = buildYahooUrl(ticker, startDate, endDate);
    List<PriceMin5> prices = new ArrayList<>();
    int retries = 0;

    while (retries < MAX_RETRIES) {
      try {
        ResponseEntity<String> response = executeRequest(url);
        if (response.getBody() == null) {
          throw new RuntimeException("Empty response from Yahoo API");
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        prices = extractPriceData(root, dbTicker);

        if (!prices.isEmpty()) {
          break; // Success, exit retry loop
        } else {
          logger.warn("Empty data received for {}, attempt {}/{}",
              ticker, retries + 1, MAX_RETRIES);
        }

      } catch (HttpClientErrorException.TooManyRequests e) {
        logger.warn("Rate limit hit for {}, attempt {}/{}", ticker, retries + 1, MAX_RETRIES);
        if (retries == MAX_RETRIES - 1) {
          throw new RuntimeException("Rate limit exceeded after " + MAX_RETRIES + " retries");
        }
        randomDelay(INITIAL_RETRY_DELAY_MS * (retries + 1),
            INITIAL_RETRY_DELAY_MS * (retries + 2));

      } catch (Exception e) {
        // Check if this is a "Data doesn't exist" error from Yahoo API
        if (e.getMessage() != null && e.getMessage().contains("Data doesn't exist")) {
          // This is an expected error for new securities - just return empty list
          logger.info("No data exists for {} in this date range (security may not have been trading yet)", ticker);
          return new ArrayList<>();
        }

        logger.error("Error downloading data for {} (attempt {}/{}): {}",
            ticker, retries + 1, MAX_RETRIES, e.getMessage());
        if (retries == MAX_RETRIES - 1) {
          throw new RuntimeException("Failed to download data: " + e.getMessage(), e);
        }
        randomDelay(INITIAL_RETRY_DELAY_MS, INITIAL_RETRY_DELAY_MS * 2);
      }
      retries++;
    }

    return prices;
  }

  private ResponseEntity<String> executeRequest(String url) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", getRandomUserAgent());
    headers.set("Accept", "application/json");
    headers.set("Accept-Language", "en-US,en;q=0.9");
    // Don't set Accept-Encoding - let RestTemplate handle it
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<byte[]> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        entity,
        byte[].class
    );

    // Check if response is gzip compressed
    if (response.getBody() != null) {
      byte[] body = response.getBody();
      // Check for gzip magic number (0x1f 0x8b)
      if (body.length >= 2 && (body[0] & 0xFF) == 0x1F && (body[1] & 0xFF) == 0x8B) {
        // Decompress gzip data
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(body));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
          byte[] buffer = new byte[1024];
          int len;
          while ((len = gzipInputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, len);
          }
          String decompressed = outputStream.toString(StandardCharsets.UTF_8);
          return new ResponseEntity<>(decompressed, response.getHeaders(), response.getStatusCode());
        }
      }
      // Not gzipped, just convert to string
      return new ResponseEntity<>(new String(body, StandardCharsets.UTF_8),
          response.getHeaders(), response.getStatusCode());
    }
    throw new RuntimeException("Empty response body from Yahoo API");
  }

  private String buildYahooUrl(String ticker, LocalDate startDate, LocalDate endDate) {
    long startTime = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
    long endTime = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);

    return YAHOO_BASE_URL + ticker +
        "?period1=" + startTime +
        "&period2=" + endTime +
        "&interval=5m" +
        "&events=history" +
        "&includeAdjustedClose=true";
  }

  private List<PriceMin5> extractPriceData(JsonNode root, String ticker) {
    List<PriceMin5> priceDataList = new ArrayList<>();

    JsonNode result = root.path("chart").path("result").get(0);
    JsonNode timestamp = result.path("timestamp");
    JsonNode indicators = result.path("indicators").path("quote").get(0);

    JsonNode opens = indicators.path("open");
    JsonNode highs = indicators.path("high");
    JsonNode lows = indicators.path("low");
    JsonNode closes = indicators.path("close");
    JsonNode volumes = indicators.path("volume");

    for (int i = 0; i < timestamp.size(); i++) {
      // Skip if any required value is null
      if (opens.get(i).isNull() || highs.get(i).isNull() ||
          lows.get(i).isNull() || closes.get(i).isNull()) {
        continue;
      }

      // The timestamp from Yahoo Finance is in UTC
      long timestamp_seconds = timestamp.get(i).asLong();

      // Get the UTC datetime for this timestamp
      LocalDateTime etDateTime =  LocalDateTime.ofInstant(
          Instant.ofEpochSecond(timestamp_seconds),
          ZoneId.of("America/New_York") // EST (Eastern Time)
      );

        LocalTime etTime = etDateTime.toLocalTime();

        // Log to understand what times we're getting and keeping
        logger.debug("EST time: {}, ET time: {}", etDateTime, etTime);

        // Use strict market hours filter as requested
        if (!etTime.isAfter(MARKET_OPEN.minusMinutes(1)) || !etTime.isBefore(MARKET_CLOSE)) {
          // Skip this record, it's outside market hours
          logger.debug("Filtering out record at ET time: {}", etTime);
          continue;
        }






      // IMPORTANT: Store using the explicitly created Eastern Time
      PriceMin5 priceMin5 = new PriceMin5();
      priceMin5.setId(new PriceKey(ticker, etDateTime));
      priceMin5.setOpen(opens.get(i).asDouble());
      priceMin5.setHigh(highs.get(i).asDouble());
      priceMin5.setLow(lows.get(i).asDouble());
      priceMin5.setClose(closes.get(i).asDouble());
      priceMin5.setVolume(volumes.get(i).asLong());

      priceDataList.add(priceMin5);
    }

    return priceDataList;
  }

  private void randomDelay(long minMs, long maxMs) {
    try {
      long delay = minMs + random.nextLong(maxMs - minMs + 1);
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Download interrupted", e);
    }
  }

  private String getRandomUserAgent() {
    return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
  }

  @Override
  public int fetchAndSaveDataFromDate(String ticker, LocalDate startDate) {
    LocalDate currentDate = LocalDate.now();
    int totalSaved = 0;

    while (startDate.isBefore(currentDate)) {
      YearMonth yearMonth = YearMonth.from(startDate);
      String yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

      try {
        totalSaved += fetchAndSaveData(ticker, yearMonthStr);
        startDate = startDate.plusMonths(1);

        // Add delay between months
        randomDelay(5000, 15000);

      } catch (Exception e) {
        logger.error("Failed to download data for {}-{}: {}",
            ticker, yearMonthStr, e.getMessage());
        throw e;
      }
    }

    return totalSaved;
  }

  @Override
  public String getDbTicker() {
    return dbTicker;
  }

  @Override
  public int fetchAndSaveData(String timeSeriesLabel, String ticker, String interval,
                              String yearMonth) {
    return fetchAndSaveData(ticker, yearMonth);
  }
}