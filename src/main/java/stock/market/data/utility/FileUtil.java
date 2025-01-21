package stock.market.data.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class FileUtil {
    private final Logger logger = LogManager.getLogger(FileUtil.class);

    public boolean validateIfFileExists(String fileName) {
        File f = new File(fileName);
        if (f.exists() && f.isFile() && f.length() > 0){
            logger.info("{} already exists and is populated with data, no need to create.",  fileName);
            return true;
        }
        logger.info("{} Does not exist, will need to create.",  fileName);
        return false;
    }
}
