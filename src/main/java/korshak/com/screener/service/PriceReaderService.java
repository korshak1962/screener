package korshak.com.screener.service;

import korshak.com.screener.dao.PriceMin5;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PriceReaderService {
    Page<PriceMin5> getSharePricesBetweenDates(String ticker, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Optional<PriceMin5> getSharePrice(String ticker, LocalDateTime date);
    Page<PriceMin5> getSharePrices(String ticker, Pageable pageable);

}
