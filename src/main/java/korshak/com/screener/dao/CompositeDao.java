package korshak.com.screener.dao;

import java.util.List;

public interface CompositeDao {

  void save(CompositeCase compositeCase);

  void saveAll(List<CompositeCase> compositeCases);

  void delete(CompositeCase compositeCase);

  void deleteByCaseId(String caseId);

  // Get one CompositeCase for a specific case_id (one-to-one relationship)
  CompositeCase findByCaseId(String caseId);

  // Get all CompositeCases with a specific composite_id (many case_ids can share a composite_id)
  List<CompositeCase> findByCompositeId(String compositeId);

  // Get all distinct composite_ids in the system
  List<String> findAllDistinctCompositeIds();

  // Get all case_ids associated with a specific composite_id
  List<String> findCaseIdsByCompositeId(String compositeId);

  List<CompositeCase> findAll();

  boolean existsByCaseId(String caseId);
}