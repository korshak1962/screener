package korshak.com.screener.serviceImpl;

import korshak.com.screener.dao.PriceKey;
import korshak.com.screener.dao.PriceMin5;

import korshak.com.screener.dao.PriceMin5Repository;
import korshak.com.screener.service.PriceReaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PriceReaderServiceImpl implements PriceReaderService {

    @Autowired
    private PriceMin5Repository priceMin5Repository;

    @Override
    public Page<PriceMin5> getSharePricesBetweenDates(String ticker, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return priceMin5Repository.findById_TickerAndId_DateBetween(ticker, startDate, endDate, pageable);
    }
    @Override
    public Optional<PriceMin5> getSharePrice(String ticker, LocalDateTime date) {
        return priceMin5Repository.findById(new PriceKey(ticker, date));
    }

    @Override
    public Page<PriceMin5> getSharePrices(String ticker, Pageable pageable) {
        return priceMin5Repository.findById_Ticker(ticker, pageable);
    }
}
