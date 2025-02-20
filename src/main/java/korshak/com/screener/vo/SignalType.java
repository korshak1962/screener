package korshak.com.screener.vo;


public enum SignalType {
  ShortOpen(-2),
  ShortClose(-1),
  LongClose(1),
  LongOpen(2);

  public final int value;

  SignalType(int value) {
    this.value = value;
  }
}

