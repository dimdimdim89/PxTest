package ex;

import org.junit.Test;
import task.PxService;
import task.Tuple2;
import task.impl.PxServiceImpl;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.concurrent.*;

import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public class PxServiceImplTest {

    @Test
    public void should_print_in_correct_order() throws InterruptedException {
        PxService<Object, LocalDateTime, Callable<Object>> service = PxServiceImpl.getInstance();
        LocalDateTime time = LocalDateTime.now().minusSeconds(1);
        ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
        Tuple2<LocalDateTime, Callable<Object>> t1 = new Tuple2<>(time, () -> queue.add("a"));
        Tuple2<LocalDateTime, Callable<Object>> t2 = new Tuple2<>(time, () -> queue.add("b"));
        Tuple2<LocalDateTime, Callable<Object>> t3 = new Tuple2<>(time, () -> queue.add("c"));
        Future<Object> fut1 = service.submit(t1);
        Future<Object> fut2 = service.submit(t2);
        Future<Object> fut3 = service.submit(t3);
        while (!(fut1.isDone() && fut2.isDone() && fut3.isDone())) Thread.sleep(1000);
        Iterator it = queue.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        service.shutdown();
    }

    @Test
    public void should_print_in_correct_order2() throws InterruptedException {
        PxService<Object, LocalDateTime, Callable<Object>> service = PxServiceImpl.getInstance();
        LocalDateTime time = LocalDateTime.now().minusSeconds(1);
        ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
        Tuple2<LocalDateTime, Callable<Object>> t1 = new Tuple2<>(time, () -> queue.add("a"));
        Tuple2<LocalDateTime, Callable<Object>> t2 = new Tuple2<>(time.minusSeconds(2), () -> queue.add("b"));
        Tuple2<LocalDateTime, Callable<Object>> t3 = new Tuple2<>(time, () -> queue.add("c"));
        Future<Object> fut1 = service.submit(t1);
        Future<Object> fut2 = service.submit(t2);
        Future<Object> fut3 = service.submit(t3);
        while (!(fut1.isDone() && fut2.isDone() && fut3.isDone())) Thread.sleep(1000);
        Iterator it = queue.iterator();
        assertEquals("a", it.next());
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        service.shutdown();
    }
}