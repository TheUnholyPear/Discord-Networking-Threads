import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

public class MessageHandlerTest {
    public static void main(String[] args) throws InterruptedException {
        int numberOfThreads = 4;
        CountDownLatch readyThreadCounter = new CountDownLatch(numberOfThreads);
        CountDownLatch joinThreadCounter = new CountDownLatch(numberOfThreads);

        // instantiate client threads and add them to array, and also add latches to list
        Client[] ps = new Client[numberOfThreads];
        for(int i = 0; i < numberOfThreads; i++){
            ps[i] = new Client("user" + i, readyThreadCounter, joinThreadCounter);
            ps[i].start();
        }
        readyThreadCounter.await();
        System.out.println("ready ");

        joinThreadCounter.await();
        System.out.println("completed");
    }

    public static class Client extends Thread {
        final String HOST = "localhost";
        final int PORT = 9000;
        private String username;
        private PrintWriter writer;
        private BufferedReader reader;
        private Socket socket;
        private CountDownLatch readyThreadCounter;
        private CountDownLatch joinThreadCounter;

        public Client(String username, CountDownLatch readyThreadCounter, CountDownLatch joinThreadCounter){
            try {
                this.username = username;
                this.socket = new Socket(HOST, PORT);
                this.writer = new PrintWriter(socket.getOutputStream(), true);
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.readyThreadCounter = readyThreadCounter;
                this.joinThreadCounter = joinThreadCounter;
                writer.println(username);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run(){
            // Listen for server messages in a separate thread
            Thread readerThread = new Thread(() -> {
                String message;
                try {
                    while ((message = reader.readLine()) != null) {
                        System.out.println(username + " received: " + message);
                        if(message.equals(username + " has joined the room: room1")) {
                            joinThreadCounter.countDown();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            readerThread.start();
            readyThreadCounter.countDown();
            try {
                readyThreadCounter.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(username.equals("user0")){
                writer.println("/create room1");
            } else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                writer.println("/create room" + username);
                writer.println("/join room1");
            }
            String input;

            // process client inputs
            try {
                while(true) {
                    writer.println("/join room1");
                    for (int i = 0; i < 1; i++) {
                        writer.println("/room room1 " + i);
//                        Thread.sleep(100);
                    }
                    writer.println("/leave room1");
                    Thread.sleep(100);
                }
//                    if(completedThreadCounter.getCount() != 0) {
//                        for (int i = 0; i < 4; i++) {
//                            writer.println("/room room1 " + i);
////                            incrementRef();
//                        }
//                        this.completedThreadCounter.countDown();
//                        Thread.sleep(1000);
//                    }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
