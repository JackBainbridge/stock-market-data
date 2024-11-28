package stock.market.data.service;

import org.springframework.stereotype.Service;
import stock.market.data.models.TopGainer;
import stock.market.data.repositories.TopGainerRepository;

import java.util.List;

@Service
public class TopGainerService {
    private final TopGainerRepository topGainerRepository;

    public TopGainerService(TopGainerRepository topGainerRepository) {
        this.topGainerRepository = topGainerRepository;
    }

    public List<TopGainer> getTopGainers() {
        return topGainerRepository.findAll();
    }
}
