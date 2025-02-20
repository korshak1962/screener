package korshak.com.screener.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RsiDayRepository extends JpaRepository<RsiDay, RsiKey>, RsiRepository {
}

