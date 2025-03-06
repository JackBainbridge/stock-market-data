package stock.market.data.events;

import org.springframework.context.ApplicationEvent;

public class SchemaGeneratedEvent extends ApplicationEvent {
    public SchemaGeneratedEvent(Object source) {
        super(source);
    }
}
