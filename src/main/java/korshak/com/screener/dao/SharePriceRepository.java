package korshak.com.screener.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SharePriceRepository extends JpaRepository<SharePrice, SharePriceId> {
    Page<SharePrice> findByTicker(String ticker, Pageable pageable);
    Page<SharePrice> findByTickerAndDateBetween(String ticker, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
