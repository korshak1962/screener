package korshak.com.screener.controller;

import korshak.com.screener.dao.SharePrice;
import korshak.com.screener.service.PriceReaderService;
import korshak.com.screener.service.SharePriceDownLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/share-prices")
public class SharePriceController {

    @Autowired
    private SharePriceDownLoaderService sharePriceDownLoaderService;
    @Autowired
    PriceReaderService priceReaderService;

    private CompletableFuture<Integer> downloadFuture;

    @PostMapping("/fetch")
    public ResponseEntity<String> fetchAndSaveData(
            @RequestParam String timeSeriesLabel,
            @RequestParam String ticker,
            @RequestParam String interval,
            @RequestParam String month) {
        try {
            YearMonth.parse(month); // Validate month format

            // Start the download process asynchronously
            downloadFuture = CompletableFuture.supplyAsync(() ->
                    sharePriceDownLoaderService.fetchAndSaveData(timeSeriesLabel, ticker, interval, month)
            );

            return ResponseEntity.ok("Download process started");
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid month format. Please use YYYY-MM");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error starting download: " + e.getMessage());
        }
    }

    @GetMapping("/fetch-status")
    public SseEmitter fetchStatus() {
        SseEmitter emitter = new SseEmitter();

        CompletableFuture.runAsync(() -> {
            try {
                // Wait for the download to complete
                Integer recordCount = downloadFuture.get();
                emitter.send(SseEmitter.event().name("complete").data("Download completed. " + recordCount + " records saved."));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @GetMapping("/{ticker}/{date}")
    public ResponseEntity<SharePrice> getSharePrice(
            @PathVariable String ticker,
            @PathVariable String date) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(date);
            return priceReaderService.getSharePrice(ticker, dateTime)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{ticker}")
    public ResponseEntity<Page<SharePrice>> getSharePrices(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(priceReaderService.getSharePrices(ticker, pageRequest));
    }

    @GetMapping("/{ticker}/between")
    public ResponseEntity<Page<SharePrice>> getSharePricesBetweenDates(
            @PathVariable String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(priceReaderService.getSharePricesBetweenDates(ticker, startDate, endDate, pageRequest));
    }
}
