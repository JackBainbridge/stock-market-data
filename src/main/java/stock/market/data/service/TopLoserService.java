package stock.market.data.service;

import org.springframework.stereotype.Service;
import stock.market.data.models.TopLoser;
import stock.market.data.repositories.TopLoserRepository;

import java.util.List;

@Service
public class TopLoserService {
    private final TopLoserRepository topLoserRepository;

    public TopLoserService(TopLoserRepository topLoserRepository) {
        this.topLoserRepository = topLoserRepository;
    }

    public List<TopLoser> getTopLosers() {
        return topLoserRepository.findAll();
    }
}
