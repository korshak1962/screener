package korshak.com.screener.dao;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PriceMin5Repository extends JpaRepository<PriceMin5, PriceKey> {
    List<PriceMin5> findById_Ticker(String ticker);
    List<PriceMin5> findByIdTickerOrderByIdDateAsc(String ticker);
    Page<PriceMin5> findById_Ticker(String ticker, Pageable pageable);
    Page<PriceMin5> findById_TickerAndId_DateBetween(String ticker, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
