package korshak.com.screener.dao;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class SharePriceId implements Serializable {
    private String ticker;
    private LocalDateTime date;

    // Default constructor
    public SharePriceId() {
    }

    // Constructor with parameters
    public SharePriceId(String ticker, LocalDateTime date) {
        this.ticker = ticker;
        this.date = date;
    }

    // Getters and setters
    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    // equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharePriceId that = (SharePriceId) o;
        return Objects.equals(ticker, that.ticker) &&
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, date);
    }
}
