import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HandlerImpl implements Handler {
    private LocalDateTime retryAfter;
    private int retriesCount = 0;

    private final Client client = new Client() {
        @Override
        public Response getApplicationStatus1(String id) {
            return new Response.Success("test", id);
        }

        @Override
        public Response getApplicationStatus2(String id) {
            return new Response.RetryAfter(Duration.ofMinutes(1));
        }
    };

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        if (retryAfter != null && LocalDateTime.now().isBefore(retryAfter)) {
            try {
                Thread.sleep(getMilli(retryAfter) - getMilli(LocalDateTime.now()));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        int timeout = 15;

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CompletableFuture<Response> future1 = CompletableFuture.supplyAsync(() -> client.getApplicationStatus1(id), executor);
        CompletableFuture<Response> future2 = CompletableFuture.supplyAsync(() -> client.getApplicationStatus2(id), executor);

        try {
            CompletableFuture<Object> result = CompletableFuture.anyOf(future1, future2);

            Response response = (Response) result.get(timeout, TimeUnit.SECONDS);

            if (response instanceof Response.Success) {
                retriesCount = 0;
                return new ApplicationStatusResponse.Success(id, ((Response.Success) response).applicationStatus());
            } else if (response instanceof Response.RetryAfter) {
                retryAfter = LocalDateTime.now().plus(((Response.RetryAfter) response).delay());
                return new ApplicationStatusResponse.Failure(null, getAndIncrementRetriesCount());
            }
        } catch (Exception e) {
            // Обработка исключений
        } finally {
            executor.shutdown();
        }

        return new ApplicationStatusResponse.Failure(null, getAndIncrementRetriesCount());
    }

    private int getAndIncrementRetriesCount() {
        return ++retriesCount;
    }

    private long getMilli(LocalDateTime time) {
        return time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
