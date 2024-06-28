package korshak.com.screener.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import korshak.com.screener.dao.SharePrice;
import korshak.com.screener.dao.SharePriceRepository;
import korshak.com.screener.service.SharePriceDownLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SharePriceDownLoaderServiceImpl implements SharePriceDownLoaderService {
    String apiKey;
    String baseUrl;

    public SharePriceDownLoaderServiceImpl(@Value("${alpha.apiKey}") String apiKey, @Value("${alpha.baseUrl}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Autowired
    private SharePriceRepository sharePriceRepository;

    @Autowired
    private RestTemplate restTemplate;


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
    public int fetchAndSaveData(String timeSeriesLabel, String ticker, String interval, String month) {
        String url = baseUrl + timeSeriesLabel + "&symbol=" + ticker + "&interval=" + interval + "&month=" + month
                + "&outputsize=full&apikey=" + apiKey;
        ResponseEntity<String> response = restTemplate.getForEntity(
                url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        JsonNode timeSeries = root.path("Time Series (5min)");

        List<SharePrice> intradayDataList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Iterator<Map.Entry<String, JsonNode>> entrys = timeSeries.fields();
        while (entrys.hasNext()) {
            Map.Entry<String, JsonNode> dataNode = entrys.next();
            LocalDateTime localDateTime = LocalDateTime.parse(dataNode.getKey(), formatter);
            double open = Double.parseDouble(dataNode.getValue().path("1. open").asText());
            double high = Double.parseDouble(dataNode.getValue().path("2. high").asText());
            double low = Double.parseDouble(dataNode.getValue().path("3. low").asText());
            double close = Double.parseDouble(dataNode.getValue().path("4. close").asText());
            long volume = Long.parseLong(dataNode.getValue().path("5. volume").asText());
            SharePrice sharePrice = new SharePrice(ticker, localDateTime, open, high, low, close, volume);
            intradayDataList.add(sharePrice);
            //    System.out.println("sharePrice =" + sharePrice);
        }
        List<SharePrice> saved = sharePriceRepository.saveAll(intradayDataList);
        return saved.size();
    }
}
