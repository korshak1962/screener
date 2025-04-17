package korshak.com.screener.utils;

import java.util.List;

public class Portfolios {

  //"VOO", "VBR", "VHT", "VCR", "VDC", "VGT", "VHT", "VIS", "VGT", "VHT", "VDC", "VCR", "VBR", "VOO"
  public static List<String> INDEXES = List.of("SPY", "QQQ", "TLT","GLD","MCHI","SOXL");

  public static List<String> US_WATCH =
      List.of( "GOOG", "TMDX", "NVDA", "TSLA", "AMZN", "IBIT");

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
      List.of("LI", "YMM", "KWEB", "FXI", "MOMO",  "WB",  "JD", "BABA",
           "BIDU", "YINN", "KTEC", "CHIQ");

  public static List<String> ALL = List.of( "VALE", "T", "EPAM", "LYFT",
      "YMM", "WB", "YY", "LYFT", "AAPL");

}