package stock.market.data.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import stock.market.data.models.MostTraded;
import stock.market.data.service.MostTradedService;

import java.util.List;

@Controller
@RequestMapping("/api/most-traded")
public class MostTradedController {
    private final Logger logger = LogManager.getLogger(MostTradedController.class);
    private final MostTradedService mostTradedService;

    public MostTradedController(MostTradedService mostTradedService) {
        this.mostTradedService = mostTradedService;
    }

    @GetMapping
    public ResponseEntity<List<MostTraded>> getAllMostTraded() {
        logger.info("entering getAllMostTraded");
        return new ResponseEntity<>(mostTradedService.getMostTraded(), HttpStatus.OK);
    }
}
