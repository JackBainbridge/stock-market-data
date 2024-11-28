package stock.market.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stock.market.data.models.TopGainer;

@Repository
public interface TopGainerRepository extends JpaRepository<TopGainer, Long> {
}
