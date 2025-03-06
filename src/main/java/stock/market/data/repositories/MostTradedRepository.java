package stock.market.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stock.market.data.models.MostTraded;

@Repository
public interface MostTradedRepository extends JpaRepository<MostTraded, Long> {
}
