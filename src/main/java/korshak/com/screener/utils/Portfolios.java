package korshak.com.screener.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolios {

  public static final String US = "US";
  public static final String US_WATCH = "US_WATCH";
  public static final String MOEX ="MOEX";


  public static Map<String, List<String>> NAME_TO_TICKERS = new HashMap();

  static {
    //"VOO", "VBR", "VHT", "VCR", "VDC", "VGT", "VHT", "VIS", "VGT", "VHT", "VDC", "VCR", "VBR", "VOO"
    List<String> US = List.of("QQQ","SPY","TLT", "VALE", "T", "LI", "YMM", "KWEB",
        "FXI", "MOMO", "MCHI", "LYFT", "YY","AAPL");
    NAME_TO_TICKERS.put(Portfolios.US, US);

    List<String> US_WATCH = List.of("GLD", "IBIT","GOOG","TMDX");//,"NVIDIA","TSLA","AMZN");
    NAME_TO_TICKERS.put(Portfolios.US_WATCH, US_WATCH);

    List<String> MOEX = List.of("TMOS","LKOH","NVTK","SBER","SIBN","MGNT","MRKP","T",
        "FLOT","SNGS","TRNFP");
    NAME_TO_TICKERS.put(Portfolios.MOEX, MOEX);
  }
}