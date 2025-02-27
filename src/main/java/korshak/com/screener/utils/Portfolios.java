package korshak.com.screener.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolios {

  public static final String US = "US";

  public static Map<String, List<String>> NAME_TO_TICKERS = new HashMap();

  static {
    //"VOO", "VBR", "VHT", "VCR", "VDC", "VGT", "VHT", "VIS", "VGT", "VHT", "VDC", "VCR", "VBR", "VOO"
    List<String> US = List.of("SPY", "SPXL", "TLT", "VALE", "T", "LI", "YMM", "KWEB",
        "FXI", "MOMO", "MCHI", "LYFT", "YY", "IBIT");
    NAME_TO_TICKERS.put(Portfolios.US, US);
  }
}