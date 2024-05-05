import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

class serverTestHJ {
    @Test
    void testUnSafeServer() {
        final int CLIENT_COUNT = 50;
        final CountDownLatch latch = new CountDownLatch(CLIENT_COUNT);

        try (PrintWriter logWriter = new PrintWriter(new FileWriter("server_log.txt"))) {
            for (int i = 0; i < CLIENT_COUNT; i++) {
                Thread t = new Thread(() -> {
                    try {
                        Socket socket = new Socket("localhost", 9000);
                        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        writer.println("user" + Thread.currentThread().getId());
                        reader.readLine(); // ignoring the response

                        String message = "/message nonExistent Hello from " + Thread.currentThread().getId();

                        writer.println(message);
                        logWriter.println(message);

                        writer.println("/logout");
                        reader.readLine();

                        socket.close();
                        latch.countDown();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                t.start();
            }

            latch.await();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
