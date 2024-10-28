package korshak.com.screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceMin5;
import korshak.com.screener.service.PriceReaderService;
import korshak.com.screener.service.SharePriceDownLoaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SharePriceController.class)
public class PriceMin5ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SharePriceDownLoaderService sharePriceDownLoaderService;

    @MockBean
    private PriceReaderService priceReaderService;

    @Autowired
    private ObjectMapper objectMapper;

    private PriceMin5 sampleSharePrice;
    private Page<PriceMin5> sampleSharePricePage;

    @BeforeEach
    void setUp() {
        sampleSharePrice = new PriceMin5(new PriceKey("AAPL", LocalDateTime.now()), 150.0,100,200,180,1000);
        sampleSharePricePage = new PageImpl<>(Arrays.asList(sampleSharePrice));
    }

    @Test
    void getSharePrice_ReturnsSharePrice_WhenFound() throws Exception {
        when(priceReaderService.getSharePrice(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(sampleSharePrice));

        mockMvc.perform(get("/api/share-prices/AAPL/2024-01-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(sampleSharePrice)));
    }

    @Test
    void getSharePrice_ReturnsNotFound_WhenNotFound() throws Exception {
        when(priceReaderService.getSharePrice(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/share-prices/AAPL/2024-01-01T00:00:00"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSharePrices_ReturnsPageOfSharePrices() throws Exception {
        when(priceReaderService.getSharePrices(anyString(), any(PageRequest.class)))
                .thenReturn(sampleSharePricePage);

        mockMvc.perform(get("/api/share-prices/AAPL")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "date")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(sampleSharePricePage)));
    }

    @Test
    void getSharePricesBetweenDates_ReturnsPageOfSharePrices() throws Exception {
        when(priceReaderService.getSharePricesBetweenDates(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(sampleSharePricePage);

        mockMvc.perform(get("/api/share-prices/AAPL/between")
                        .param("startDate", "2024-01-01T00:00:00")
                        .param("endDate", "2024-01-31T23:59:59")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "date")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(sampleSharePricePage)));
    }
}
