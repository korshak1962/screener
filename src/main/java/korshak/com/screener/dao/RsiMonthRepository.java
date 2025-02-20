package korshak.com.screener.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RsiMonthRepository extends JpaRepository<RsiMonth, RsiKey>, RsiRepository {
}
