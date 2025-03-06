package stock.market.data.events;

import org.springframework.context.ApplicationEvent;

public class RunnerCompletedEvent extends ApplicationEvent {
    private final Class<?> runnerClass;

    public RunnerCompletedEvent(Object source, Class<?> runnerClass) {
        super(source);
        this.runnerClass = runnerClass;
    }

    public Class<?> getRunnerClass() {
        return runnerClass;
    }
}
