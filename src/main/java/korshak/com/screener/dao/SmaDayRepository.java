package korshak.com.screener.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmaDayRepository extends JpaRepository<SmaDay, SmaKey> {
  void deleteByIdTickerAndIdLength(String ticker, int length);
}
