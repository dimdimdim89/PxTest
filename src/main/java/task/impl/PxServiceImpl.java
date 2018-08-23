package task.impl;

import task.PxService;
import task.Tuple2;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;

public class PxServiceImpl implements PxService<Object, LocalDateTime, Callable<Object>> {
    private static final int POOL_SIZE = 10;
    private ScheduledExecutorService executorService;
    private ZoneId zone;
    private static final PxService<Object, LocalDateTime, Callable<Object>> INSTANCE = new PxServiceImpl();

    private PxServiceImpl() {
        this(Executors.newScheduledThreadPool(POOL_SIZE));
    }

    private PxServiceImpl(ScheduledExecutorService executorService) {
        this.executorService = executorService;
        this.zone = ZoneId.systemDefault();
    }

    @Override
    public Future<Object> submit(Tuple2<LocalDateTime, Callable<Object>> task) {
        long delay = LocalDateTime.now().atZone(zone).toEpochSecond() - task.left.atZone(zone).toEpochSecond();
        return executorService.schedule(task.right, delay >= 0 ? 0 : -delay, SECONDS);
    }

    public static PxService<Object, LocalDateTime, Callable<Object>> getInstance() {
        return INSTANCE;
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }
}
