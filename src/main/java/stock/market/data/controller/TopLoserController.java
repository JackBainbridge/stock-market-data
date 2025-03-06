package stock.market.data.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import stock.market.data.models.TopLoser;
import stock.market.data.service.TopLoserService;

import java.util.List;

@Controller
@RequestMapping("/api/top-losers")
public class TopLoserController {
    private final Logger logger = LogManager.getLogger(TopLoserController.class);
    private final TopLoserService topLoserService;

    public TopLoserController(TopLoserService topLoserService) {
        this.topLoserService = topLoserService;
    }

    @GetMapping
    public ResponseEntity<List<TopLoser>> getAllTopLosers() {
        logger.info("entering getAllTopLosers");
        return new ResponseEntity<>(topLoserService.getTopLosers(), HttpStatus.OK);
    }

}
