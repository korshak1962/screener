package korshak.com.screener.service.strategy;

import java.util.Map;
import java.util.Set;
import korshak.com.screener.dao.Param;

public interface Configurable {
  Set<String> getParamNames();
  Map<String, Param> getParams();

  void configure(Map<String, Param> nameToParam);
}
