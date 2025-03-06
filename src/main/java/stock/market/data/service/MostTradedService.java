package stock.market.data.service;

import org.springframework.stereotype.Service;
import stock.market.data.models.MostTraded;
import stock.market.data.repositories.MostTradedRepository;

import java.util.List;

@Service
public class MostTradedService {
    private final MostTradedRepository mostTradedRepository;

    public MostTradedService(MostTradedRepository mostTradedRepository) {
        this.mostTradedRepository = mostTradedRepository;
    }

    public List<MostTraded> getMostTraded() {
        return mostTradedRepository.findAll();
    }
}
