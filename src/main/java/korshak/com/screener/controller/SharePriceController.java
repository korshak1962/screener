package korshak.com.screener.controller;

import korshak.com.screener.dao.SharePrice;
import korshak.com.screener.service.PriceReaderService;
import korshak.com.screener.service.SharePriceDownLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/share-prices")
public class SharePriceController {

    @Autowired
    private SharePriceDownLoaderService sharePriceDownLoaderService;
    @Autowired
    PriceReaderService priceReaderService;

    @GetMapping("/{ticker}/{date}")
    public ResponseEntity<SharePrice> getSharePrice(
            @PathVariable String ticker,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return priceReaderService.getSharePrice(ticker, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
/*
    @PutMapping("/{ticker}/{date}")
    public ResponseEntity<SharePrice> updateSharePrice(
            @PathVariable String ticker,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date,
            @RequestBody SharePrice sharePrice) {
        if (!ticker.equals(sharePrice.getTicker()) || !date.equals(sharePrice.getDate())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(sharePriceService.updateSharePrice(sharePrice));
    }

    @DeleteMapping("/{ticker}/{date}")
    public ResponseEntity<Void> deleteSharePrice(
            @PathVariable String ticker,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        sharePriceService.deleteSharePrice(ticker, date);
        return ResponseEntity.noContent().build();
    }
 */
}
