package korshak.com.screener.dao;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompositeDaoImpl implements CompositeDao {

  private final CompositeRepository compositeRepository;

  @Autowired
  public CompositeDaoImpl(CompositeRepository compositeIdRepository) {
    this.compositeRepository = compositeIdRepository;
  }

  @Override
  public void save(CompositeCase compositeCase) {
    compositeRepository.save(compositeCase);
  }

  @Override
  public void saveAll(List<CompositeCase> compositeCases) {
    compositeRepository.saveAll(compositeCases);
  }

  @Override
  public void delete(CompositeCase compositeCase) {
    compositeRepository.delete(compositeCase);
  }

  @Override
  public void deleteByCaseId(String caseId) {
    compositeRepository.deleteByCaseId(caseId);
  }

  @Override
  public CompositeCase findByCaseId(String caseId) {
    return compositeRepository.findByCaseId(caseId);
  }

  @Override
  public List<CompositeCase> findByCompositeId(String compositeId) {
    return compositeRepository.findByCompositeId(compositeId);
  }

  @Override
  public List<String> findAllDistinctCompositeIds() {
    return compositeRepository.findAllDistinctCompositeIds();
  }

  @Override
  public List<String> findCaseIdsByCompositeId(String compositeId) {
    return compositeRepository.findCaseIdsByCompositeId(compositeId);
  }

  @Override
  public List<CompositeCase> findAll() {
    return compositeRepository.findAll();
  }

  @Override
  public boolean existsByCaseId(String caseId) {
    return compositeRepository.existsByCaseId(caseId);
  }
}