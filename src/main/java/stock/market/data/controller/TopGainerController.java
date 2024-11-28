package stock.market.data.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import stock.market.data.models.TopGainer;
import stock.market.data.service.TopGainerService;

import java.util.List;

@Controller
@RequestMapping("/api/top-gainers")
public class TopGainerController {
    private final Logger logger = LogManager.getLogger(TopGainerController.class);
    private final TopGainerService topGainerService;

    public TopGainerController(TopGainerService topGainerService) {
        this.topGainerService = topGainerService;
    }

    @GetMapping
    public ResponseEntity<List<TopGainer>> getAllTopGainers() {
        logger.info("entering getAllTopGainers");
        return new ResponseEntity<>(topGainerService.getTopGainers(), HttpStatus.OK);
    }
}
