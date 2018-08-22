package task;

import java.util.concurrent.Future;

public interface PxService<R, A, B> {
    Future<R> submit(Tuple2<A, B> task);

    void shutdown();
}
