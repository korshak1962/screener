package korshak.com.screener.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CompositeRepository extends JpaRepository<CompositeCase, CompositeCase.CompositeCaseId> {

  // Find all cases with a specific composite_id
  List<CompositeCase> findByCompositeId(String compositeId);

  // Find the unique CompositeCase for a specific case_id (one-to-one relationship)
  CompositeCase findByCaseId(String caseId);

  // Delete CompositeCase by case_id
  void deleteByCaseId(String caseId);

  // Find if there is a CompositeCase for this case_id
  boolean existsByCaseId(String caseId);

  // Find all distinct composite_ids
  @Query("SELECT DISTINCT c.compositeId FROM CompositeCase c")
  List<String> findAllDistinctCompositeIds();

  // Find all case_ids for a specific composite_id
  @Query("SELECT c.caseId FROM CompositeCase c WHERE c.compositeId = :compositeId")
  List<String> findCaseIdsByCompositeId(@Param("compositeId") String compositeId);
}