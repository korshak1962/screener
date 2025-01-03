package korshak.com.screener.dao;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
interface PriceMin5Repository extends JpaRepository<PriceMin5, PriceKey> {
    List<PriceMin5> findById_Ticker(String ticker);
    List<PriceMin5> findByIdTickerOrderByIdDateAsc(String ticker);
    Page<PriceMin5> findById_Ticker(String ticker, Pageable pageable);
    Page<PriceMin5> findById_TickerAndId_DateBetween(String ticker, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    List<PriceMin5> findByIdTickerAndIdDateBetweenOrderByIdDateAsc(
        String ticker,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    @Query("SELECT DISTINCT p.id.ticker FROM PriceMin5 p")
    Set<String> findUniqueTickers();
}