package korshak.com.screener.serviceImpl.chart;

import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.vo.Signal;
import korshak.com.screener.vo.SignalType;
import korshak.com.screener.vo.Trade;
import org.jfree.data.time.*;
import org.jfree.data.time.ohlc.*;
import java.util.*;
import java.util.stream.Collectors;
import org.jfree.data.xy.OHLCDataset;

public class DatasetFactory {
  public OHLCDataset createCandlestickDataset(List<? extends BasePrice> prices) {
    OHLCSeries series = new OHLCSeries("Price");

    for (BasePrice price : prices) {
      series.add(
          new Millisecond(java.sql.Timestamp.valueOf(price.getId().getDate())),
          price.getOpen(),
          price.getHigh(),
          price.getLow(),
          price.getClose()
      );
    }

    OHLCSeriesCollection dataset = new OHLCSeriesCollection();
    dataset.addSeries(series);
    return dataset;
  }

  public TimeSeriesCollection createSignalDataset(List<Signal> signals) {
    TimeSeriesCollection dataset = new TimeSeriesCollection();
    TimeSeries buySeries = new TimeSeries("Buy");
    TimeSeries sellSeries = new TimeSeries("Sell");

    for (Signal signal : signals) {
      Millisecond timePoint = new Millisecond(
          java.sql.Timestamp.valueOf(signal.getDate())
      );

      if (signal.getAction() == SignalType.Buy) {
        buySeries.add(timePoint, signal.getPrice());
      } else if (signal.getAction() == SignalType.Sell) {
        sellSeries.add(timePoint, signal.getPrice());
      }
    }

    dataset.addSeries(buySeries);
    dataset.addSeries(sellSeries);
    return dataset;
  }

  public TimeSeriesCollection createSmaDataset(List<? extends BaseSma> smaList) {
    TimeSeriesCollection dataset = new TimeSeriesCollection();

    Map<Integer, List<BaseSma>> smasByLength = smaList.stream()
        .collect(Collectors.groupingBy(sma -> sma.getId().getLength()));

    for (Map.Entry<Integer, List<BaseSma>> entry : smasByLength.entrySet()) {
      TimeSeries smaSeries = new TimeSeries("SMA(" + entry.getKey() + ")");

      for (BaseSma sma : entry.getValue()) {
        smaSeries.add(
            new Millisecond(java.sql.Timestamp.valueOf(sma.getId().getDate())),
            sma.getValue()
        );
      }

      dataset.addSeries(smaSeries);
    }

    return dataset;
  }

  public TradeHistogramDataset createTradeHistogramDataset(List<Trade> trades) {
    return new TradeHistogramDataset(trades);
  }
}