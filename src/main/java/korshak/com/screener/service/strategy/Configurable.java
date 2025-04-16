package korshak.com.screener.service.strategy;

import java.util.Map;
import korshak.com.screener.dao.Param;

public interface Configurable {
  Map<String, Param> getParams();

  void configure(Map<String, Param> nameToParam);
}
