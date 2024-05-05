import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class FRaceTest {

    public static void main(String[] args) throws InterruptedException {
        int numberOfThreads = 2;
        CountDownLatch readyThreadCounter = new CountDownLatch(numberOfThreads);
        CountDownLatch completedThreadCounter = new CountDownLatch(numberOfThreads);

        // instantiate client threads and add them to array
        Client[] ps = new Client[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            ps[i] = new Client("user" + i, readyThreadCounter, completedThreadCounter);
            ps[i].start();
        }

        // run client threads
        for (int i = 0; i < numberOfThreads; i++) {
            // send list of commands to server
            ps[i].addCommand("/Vprofile user0");
            ps[i].addCommand("/Eprofile user0 Nickname SillyGoose" + i);
            ps[i].addCommand("/Eprofile user0 AboutMe I am the silliest Goose" + i);
            ps[i].addCommand("/Vprofile user0");
            // ps[i].sleep(5000);
        }
        completedThreadCounter.await();
    }

    public static class Client extends Thread {
        final String HOST = "localhost";
        final int PORT = 9000;
        private String username;
        private PrintWriter writer;
        private BufferedReader reader;
        private Socket socket;
        private ArrayList<String> command;
        private CountDownLatch readyThreadCounter;
        private CountDownLatch completedThreadCounter;

        public Client(String username, CountDownLatch readyThreadCounter, CountDownLatch completedThreadCounter) {
            try {
                this.username = username;
                this.socket = new Socket(HOST, PORT);
                this.writer = new PrintWriter(socket.getOutputStream(), true);
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.command = new ArrayList<>();
                this.readyThreadCounter = readyThreadCounter;
                this.completedThreadCounter = completedThreadCounter;
                writer.println(username);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            // Listen for server messages in a separate thread
            Thread readerThread = new Thread(() -> {
                String message;
                try {
                    while ((message = reader.readLine()) != null) {
                        System.out.println(username + " received: " + message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            readerThread.start();
            readyThreadCounter.countDown();

            // Send messages to server
            String input;
            try {
                // process client inputs
                while (!command.isEmpty()) {
                    input = command.get(0);
                    command.remove(0);
                    System.out.println(username + " inputting: " + input);
                    if (input.equals("/rooms")) {
                        writer.println("/rooms");
                    } else if (input.equals("/quit") || input.equals("/logout")) {
                        writer.println("/logout");
                        socket.close();
                        System.exit(0);
                    } else if (input.startsWith("/whisper")) {
                        String[] parts = input.split(" ");
                        if (parts.length >= 3) {
                            String targetUser = parts[1];
                            String message = String.join(" ", parts)
                                    .substring(parts[0].length() + parts[1].length() + 2);
                            writer.println("/whisper " + targetUser + " " + message);
                        } else {
                            System.out.println("Usage: /whisper <username> <message>");
                        }
                    } else if (input.startsWith("/Eprofile")) {
                        // Split the input to get the target user's username, profile element, and new
                        // value
                        String[] parts = input.split(" ");
                        if (parts.length >= 4) {
                            String targetUser = parts[1];
                            String profileElement = parts[2];
                            String change = String.join(" ", Arrays.copyOfRange(parts, 3, parts.length));

                            // Send the command to edit the profile to the server
                            writer.println("/Eprofile");
                            writer.println(targetUser);
                            writer.println(profileElement);
                            writer.println(change);
                        } else {
                            System.out.println("Usage: /Eprofile <username> <profile element> <new value>");
                        }

                    } else if (input.startsWith("/Vprofile")) { // input command to view a profile
                        String[] viewProfileParts = input.split(" ");
                        if (viewProfileParts.length == 2) {
                            writer.println(viewProfileParts[0]);
                            writer.println(viewProfileParts[1]);
                        } else {
                            System.out.println("Usage: /Vprofile <username>");
                        }
                    } else {
                        writer.println(input);
                    }
                    completedThreadCounter.countDown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void addCommand(String input) {
            this.command.add(input);
        }
    }
}
