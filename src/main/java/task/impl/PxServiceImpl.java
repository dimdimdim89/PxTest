package task.impl;

import task.PxService;
import task.Tuple2;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.SECONDS;

public class PxServiceImpl implements PxService<Object, LocalDateTime, Callable<Object>> {
    private AtomicLong order;
    private PriorityBlockingQueue<Task> queue;
    private ExecutorService service;
    private ScheduledExecutorService executor;
    private ZoneId zone;
    private Map<Integer, Condition> conditionMap;
    private Map<Integer, Lock> lockMap;
    private Map<Integer, Future<Object>> futures;
    private static final PxServiceImpl INSTANCE = new PxServiceImpl();

    private PxServiceImpl() {
        order = new AtomicLong(0);
        queue = new PriorityBlockingQueue<>();
        service = Executors.newSingleThreadExecutor();
        executor = Executors.newSingleThreadScheduledExecutor();
        zone = ZoneId.systemDefault();
        conditionMap = new ConcurrentHashMap<>();
        lockMap = new ConcurrentHashMap<>();
        futures = new ConcurrentHashMap<>();
        service.submit(() -> {
            Lock lock;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Task task = queue.take();
                    int key = task.hashCode();
                    lock = lockMap.get(key);
                    lock.lock();
                    long delay = LocalDateTime.now().atZone(zone).toEpochSecond() - task.time.atZone(zone).toEpochSecond();
                    ScheduledFuture<Object> future = executor.schedule(task.job, delay >= 0 ? 0 : -delay, SECONDS);
                    futures.put(key, future);
                    conditionMap.get(key).signal();
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static PxServiceImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public Future<Object> submit(Tuple2<LocalDateTime, Callable<Object>> tuple) {
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        Task task = new Task(tuple.left, tuple.right, order.incrementAndGet());
        int key = task.hashCode();
        conditionMap.put(key, condition);
        lockMap.put(key, lock);
        lock.lock();
        queue.offer(task);
        try {
            condition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lock.unlock();
        conditionMap.remove(key);
        lockMap.remove(key);
        return futures.remove(key);
    }

    @Override
    public void shutdown() {
        service.shutdown();
    }

    private static class Task implements Comparable<Task> {
        private LocalDateTime time;
        private Callable<Object> job;
        private long order;

        Task(LocalDateTime time, Callable<Object> job, long order) {
            this.time = time;
            this.job = job;
            this.order = order;
        }

        @Override
        public int compareTo(Task o) {
            int result = time.compareTo(o.time);
            if (result == 0) {
                return Long.compare(order, o.order);
            } else {
                return result;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Task task = (Task) o;
            return order == task.order &&
                    Objects.equals(time, task.time) &&
                    Objects.equals(job, task.job);
        }

        @Override
        public int hashCode() {
            return Objects.hash(time, job, order);
        }
    }
}
