package stock.market.data.models;

import org.springframework.stereotype.Component;

@Component
public class ColumnInformationSchemaFile {
    public int maxLength = 0;
    public int maxDecimalLength = 0;
    public boolean isNumeric = true;
}
