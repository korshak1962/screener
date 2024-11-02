package korshak.com.screener.serviceImpl;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import korshak.com.screener.dao.BasePrice;
import korshak.com.screener.dao.BaseSma;
import korshak.com.screener.dao.SmaDay;
import korshak.com.screener.dao.SmaHour;
import korshak.com.screener.dao.SmaKey;
import korshak.com.screener.dao.SmaMonth;
import korshak.com.screener.dao.SmaWeek;
import korshak.com.screener.dao.TimeFrame;
import korshak.com.screener.service.PriceDao;
import korshak.com.screener.service.SmaCalculationService;
import korshak.com.screener.service.SmaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Updated service using the factory
@Service
public class SmaCalculationServiceImpl implements SmaCalculationService {
  private final PriceDao priceDao;
  private final SmaDao smaDao;

  @Autowired
  public SmaCalculationServiceImpl(PriceDao priceDao, SmaDao smaDao) {
    this.priceDao = priceDao;
    this.smaDao = smaDao;
  }

  @Transactional
  public void calculateSMA(
      String ticker,
      int length,
      LocalDateTime startDate,
      LocalDateTime endDate,
      TimeFrame timeFrame) {

    smaDao.deleteByTickerAndLength(ticker, length, timeFrame);

    List<? extends BasePrice> prices = priceDao.findByDateRange(
        ticker, startDate, endDate, timeFrame);

    calculateAndSaveSMA(prices, ticker, length, timeFrame);
  }

  @Override
  @Transactional
  public void calculateSMA(String ticker, int length, TimeFrame timeFrame) {
    smaDao.deleteByTickerAndLength(ticker, length, timeFrame);

    List<? extends BasePrice> prices = priceDao.findAllByTicker(ticker, timeFrame);
    if (prices.isEmpty()) {
      throw new RuntimeException("prices for timeframe." + timeFrame + " not found");
    }
    calculateAndSaveSMA(prices, ticker, length, timeFrame);
  }

  private void calculateAndSaveSMA(
      List<? extends BasePrice> prices,
      String ticker,
      int length,
      TimeFrame timeFrame) {

    if (prices.size() < length) {
      return;
    }

    List<BaseSma> smaResults = new ArrayList<>();
    double sum = 0;

    // Calculate initial sum
    for (int i = 0; i < length; i++) {
      sum += prices.get(i).getClose();
    }

    // Calculate SMAs
    for (int i = length - 1; i < prices.size(); i++) {

      BaseSma sma = getBaseSma(timeFrame);

      SmaKey smaKey = new SmaKey(
          ticker,
          prices.get(i).getId().getDate(),
          length
      );
      sma.setId(smaKey);
      sma.setValue(sum / length);
      smaResults.add(sma);

      if (i < prices.size() - 1) {
        sum = sum - prices.get(i - length + 1).getClose()
            + prices.get(i + 1).getClose();
      }

      if (smaResults.size() >= 1000) {
        smaDao.saveAll(smaResults, timeFrame);
        smaResults.clear();
      }
    }

    if (!smaResults.isEmpty()) {
      smaDao.saveAll(smaResults, timeFrame);
    }
  }

  private static BaseSma getBaseSma(TimeFrame timeFrame) {
    BaseSma sma = switch (timeFrame) {
      case HOUR -> new SmaHour();
      case DAY -> new SmaDay();
      case WEEK -> new SmaWeek();
      case MONTH -> new SmaMonth();
    };
    return sma;
  }
}