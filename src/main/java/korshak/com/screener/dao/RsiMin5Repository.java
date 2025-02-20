package korshak.com.screener.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RsiMin5Repository extends JpaRepository<RsiMin5, RsiKey>, RsiRepository {
}

