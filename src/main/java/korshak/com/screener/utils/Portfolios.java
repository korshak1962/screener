package korshak.com.screener.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolios {

  //"VOO", "VBR", "VHT", "VCR", "VDC", "VGT", "VHT", "VIS", "VGT", "VHT", "VDC", "VCR", "VBR", "VOO"
  public static List<String> US = List.of("QQQ", "SPY", "TLT", "VALE", "T", "LYFT", "AAPL");

  public static List<String> US_WATCH =
      List.of("GLD", "IBIT", "GOOG", "TMDX", "NVDA", "TSLA", "AMZN");

  public static List<String> US_SECTOR_ETF =
      List.of("XLB", "XLU", "XLI", "XLC", "XLK", "XLE", "XLP", "XLF", "XLV",
          "XME", "XLY", "XLRE", "SOXL");

  public static List<String> MOEX =
      List.of("TMOS", "LKOH", "NVTK", "SBER", "SIBN", "MGNT", "T", "ROSN",
          "FLOT", "TRNFP", "DIVD", "BOND");

  public static List<String> EM =
      List.of("EEMV", "IEMG", "EWZ", "ARGT", "EWW", "GXG", "ECH", "AAXJ");

  //"TCEHY", "XIACF","YY",
  public static List<String> CHINA =
      List.of("LI", "YMM", "KWEB", "FXI", "MOMO", "MCHI", "WB",  "JD", "BABA",
           "BIDU", "YINN", "KTEC", "CHIQ");

  public static List<String> ALL = List.of("SPY", "QQQ", "SOXL", "TLT", "VALE", "T", "EPAM", "LYFT",
      "YMM", "MCHI", "WB", "YY");

}