package stock.market.data.models;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "MOST_ACTIVELY_TRADED")
public class MostTraded {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "TICKER")
    private String ticker;
    @Column(name = "PRICE")
    private BigDecimal price;
    @Column(name = "CHANGE_AMOUNT")
    private BigDecimal changeAmount;
    @Column(name = "CHANGE_PERCENTAGE")
    private BigDecimal changePercentage;
    @Column(name = "VOLUME")
    private Long volume;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount) {
        this.changeAmount = changeAmount;
    }

    public BigDecimal getChangePercentage() {
        return changePercentage;
    }

    public void setChangePercentage(BigDecimal changePercentage) {
        this.changePercentage = changePercentage;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }
}
