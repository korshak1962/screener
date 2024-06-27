package korshak.com.screener.service;

import korshak.com.screener.dao.SharePrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PriceReaderService {
    Page<SharePrice> getSharePricesBetweenDates(String ticker, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Optional<SharePrice> getSharePrice(String ticker, LocalDateTime date);
    Page<SharePrice> getSharePrices(String ticker, Pageable pageable);

}
