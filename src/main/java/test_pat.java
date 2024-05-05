import java.io.*;
import java.util.concurrent.CountDownLatch;

public class test_pat {

    public static void main(String[] args) {
        System.out.println("\n\n\nStart:\n");

        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> runProgram("Instance1", latch)).start();
        new Thread(() -> runProgram("Instance2", latch)).start();

        latch.countDown();
    }

    public static void runProgram(String instanceName, CountDownLatch latch) {

        ProcessBuilder processBuilder = new ProcessBuilder("java",
                "H:\\cis\\windows\\IdeaProjects\\cs313\\client.java");

        try {

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());

            Thread outputThread = new Thread(() -> {
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        System.out.println(instanceName + " Output: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            outputThread.start();

            latch.await();

            writer.write("USER\n");
            writer.flush();
            Thread.sleep(500);
            writer.write("/NewLobby " + "TEST1" + "\n");
            writer.flush();
            Thread.sleep(500);
            writer.write("/JoinLobby " + "TEST1" + "\n");
            writer.flush();
            Thread.sleep(500);
            writer.write("/startGame Hangman\n");
            writer.flush();
            Thread.sleep(500);

            int exitCode = process.waitFor();
            System.out.println(instanceName + " Process exited with code: " + exitCode);

            outputThread.interrupt();
            outputThread.join();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
