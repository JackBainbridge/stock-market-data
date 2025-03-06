package stock.market.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import stock.market.data.models.TopLoser;

@Service
public interface TopLoserRepository extends JpaRepository<TopLoser, Long> {
}
