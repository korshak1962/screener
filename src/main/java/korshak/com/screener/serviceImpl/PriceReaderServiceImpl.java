package korshak.com.screener.serviceImpl;

import korshak.com.screener.dao.SharePrice;
import korshak.com.screener.dao.SharePriceId;
import korshak.com.screener.dao.SharePriceRepository;
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
    private SharePriceRepository sharePriceRepository;

    @Override
    public Page<SharePrice> getSharePricesBetweenDates(String ticker, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return sharePriceRepository.findByTickerAndDateBetween(ticker, startDate, endDate, pageable);
    }
    @Override
    public Optional<SharePrice> getSharePrice(String ticker, LocalDateTime date) {
        return sharePriceRepository.findById(new SharePriceId(ticker, date));
    }

    @Override
    public Page<SharePrice> getSharePrices(String ticker, Pageable pageable) {
        return sharePriceRepository.findByTicker(ticker, pageable);
    }
}
