package korshak.com.screener.vo;


public enum SignalType {
  ShortOpen(-2),
  ShortClose(-1),
  LongClose(1),
  LongOpen(2);

  SignalType(int value) {
    this.value = value;
  }
  public final int value;
}

