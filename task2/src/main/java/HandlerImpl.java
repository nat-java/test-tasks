import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class HandlerImpl implements Handler {

    Client client = new Client() {
        int count = 0;
        @Override
        public Event readData() {
            return new Event(Arrays.asList(new Address("datacenter" + ++count, "nodeId" + count),
                new Address("datacenter" + ++count, "nodeId" + count)),
                new Payload("origin", "origin".getBytes()));
        }

        @Override
        public Result sendData(Address dest, Payload payload) {
            return Result.ACCEPTED;
        }
    };

    @Override
    public Duration timeout() {
        return Duration.ofSeconds(10);
    }

    @Override
    public void performOperation() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        while (true) {
            Event event = client.readData();
            System.out.println("Get " + event);

            if (event != null) {
                List<Address> recipients = event.recipients();
                Payload payload = event.payload();

                for (Address recipient : recipients) {
                    executorService.submit(() ->
                        sendData(payload, recipient)
                    );
                }
            }
        }
    }

    private void sendData(Payload payload, Address recipient) {
        try {
            System.out.println("Sending " + payload + " to " + recipient);

            Result result = client.sendData(recipient, payload);
            if (result == Result.REJECTED) {
                Thread.sleep(timeout().toMillis());
                sendData(payload, recipient);
            }

            System.out.println("Successfully sent " + payload + " to " + recipient);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
}
