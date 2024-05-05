import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.Scanner;

public class client {
    private static final String HOST = "192.168.8.45";
    private static final int PORT = 9000;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(HOST, PORT);

            avutil.av_log_set_level(avutil.AV_LOG_ERROR); // get rid of the metadata information when streaming

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter username: ");
            String userName = consoleReader.readLine();
            writer.println(userName);

            Thread listenerThread = new Thread(() -> {
                String message;
                try {
                    while ((message = reader.readLine()) != null) {
                        if (message.startsWith("/waitForP2P")) {
                            String[] parts = message.split(" ");
                            if (parts.length == 5) {
                                String senderUserName = parts[1];
                                String senderAddress = parts[2];
                                int senderPort = Integer.parseInt(parts[3]);
                                String fileName = parts[4];

                                System.out.println(senderUserName + " wants to send you a file Accept? (y/n)");
                                String response = consoleReader.readLine().trim().toLowerCase();

                                System.out.println("You said " + response);

                                receiveFileP2P(senderUserName, senderAddress, senderPort, fileName, writer, response); // Pass consoleReader
                            }
                        } else if (message.startsWith("/fileResponse")) {
                            String[] parts = message.split(" ");
                            if ("decline".equals(parts[1])) {
                                System.out.println("File transfer declined");

                            } else if ("accept".equals(parts[1])) {
                                System.out.println("File transfer accepted. Proceeding...");

                            }

                        } else if (message.startsWith("/steamP2P")) {
                            String[] parts = message.split(" ");
                            if (parts.length == 7) {
                                String senderUserName = parts[1];
                                String senderAddress = parts[2];
                                int senderPort = Integer.parseInt(parts[3]);
                                String fileName = parts[4];
                                int segments = Integer.parseInt(parts[5]);
                                int framerate = Integer.parseInt(parts[6]);


                                System.out.println(senderUserName + " wants to stream to you Accept? (y/n)");
                                String response = consoleReader.readLine().trim().toLowerCase();

                                System.out.println("You said " + response);

                                receiveP2PVideo(senderUserName, senderAddress, senderPort, fileName, writer, response,
                                        framerate); // Pass consoleReader
                            }
                        } else if (message.startsWith("/streamResponse")) {
                            String[] parts = message.split(" ");
                            if ("decline".equals(parts[1])) {
                                System.out.println("Video transfer declined");

                            } else if ("accept".equals(parts[1])) {
                                System.out.println("Video transfer accepted. Proceeding...");

                            }
                        } else if (message.startsWith("/callP2P")) {
                            String[] parts = message.split(" ");
                            if (parts.length == 4) {
                                String senderUserName = parts[1];
                                String senderAddress = parts[2];
                                int senderPort = Integer.parseInt(parts[3]);

                                System.out.println(senderUserName + " wants to stream to you Accept? (y/n)");
                                String response = consoleReader.readLine().trim().toLowerCase();

                                System.out.println("You said " + response);

                                receiveCallP2PVideo(senderUserName, senderAddress, senderPort, writer, response); // Pass
                                // consoleReader
                            }
                        } else if (message.startsWith("/callResponse")) {
                            String[] parts = message.split(" ");
                            if ("decline".equals(parts[1])) {
                                System.out.println("Video transfer declined");
                                stopSignal = true;

                            } else if ("accept".equals(parts[1])) {
                                System.out.println("Video transfer accepted. Proceeding...");

                            }
                        }

                        else {
                            System.out.println(message);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            listenerThread.start();

            try {
                File msgLogs = new File(userName);
                Scanner logReader = new Scanner(msgLogs);

                while (logReader.hasNextLine()) {
                    String data = logReader.nextLine();
                    System.out.println(data);
                }
                logReader.close();
                msgLogs.delete();
            } catch (FileNotFoundException e) {
                System.out.println("No logs to read from");
            }

            String input;
            while ((input = consoleReader.readLine()) != null) {
                switch (input.split(" ")[0]) {
                    case "/status":
                        writer.println("/status");
                        break;
                    case "/quit":
                        writer.println("/logout");
                        socket.close();
                        System.exit(0);
                        break;
                    case "/logout":
                        writer.println("/logout");
                        socket.close();
                        System.exit(0);
                        break;
                    case "/message":
                        String[] parts = input.split(" ");
                        if (parts.length >= 3) {
                            String targetUser = parts[1];
                            String messageToSend = input.substring(input.indexOf(' ', input.indexOf(' ') + 1) + 1);
                            writer.println("/message " + targetUser + " " + messageToSend);
                        } else {
                            System.out.println("Usage: /message <username> <message>");
                        }
                        break;
                    case "/file":
                        parts = input.split(" ");
                        if (parts.length == 3) {
                            String targetUser = parts[1];
                            String filePath = parts[2];
                            File file = new File(filePath);
                            if (!file.exists()) {
                                System.out.println("File does not exist: " + filePath);
                                break;
                            }
                            new Thread(() -> sendFileP2P(targetUser, filePath, writer)).start();
                        } else {
                            System.out.println("Usage: /file <username> <file path>");
                        }
                        break;
                    case "/stream":
                        parts = input.split(" ");
                        if (parts.length == 3) {
                            String targetUser = parts[1];
                            String filePath = parts[2];
                            File file = new File(filePath);
                            if (!file.exists()) {
                                System.out.println("File does not exist: " + filePath);
                                break;
                            }
                            new Thread(() -> streamP2PVideo(targetUser, filePath, writer)).start();
                        } else {
                            System.out.println("Usage: /stream <username> <file path>");
                        }
                        break;
                    case "/call":
                        parts = input.split(" ");
                        if (parts.length == 2) {
                            String targetUser = parts[1];
                            callP2PVideo(targetUser, writer);
                        } else {
                            System.out.println("Usage: /call <username>");
                        }
                        break;


                    default:
                        writer.println(input);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static volatile boolean stopSignal = false;

    private static void sendFileP2P(String targetUser, String filePath, PrintWriter writer) {
        System.out.println("Waiting for Responce:");
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int localPort = serverSocket.getLocalPort();
            String address = InetAddress.getLocalHost().getHostAddress();
            writer.println(
                    "/file " + targetUser + " " + new File(filePath).getName() + " " + localPort + " " + address);

            try (Socket p2pSocket = serverSocket.accept();
                 BufferedOutputStream out = new BufferedOutputStream(p2pSocket.getOutputStream());
                 BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(filePath))) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
                System.out.println("File sent successfully.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to send file: " + e.getMessage());
        }
    }

    private static void receiveFileP2P(String senderUserName, String senderAddress, int senderPort, String fileName,
                                       PrintWriter writer, String response) {
        if ("y".equals(response)) {
            writer.println("/fileResponse " + senderUserName + " accept " + fileName);
            // Here, initiate the connection to receive the file since acceptance has been
            // confirmed.

            try (Socket socket = new Socket(senderAddress, senderPort);
                 BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
                File downloadsDir = new File("Downloads" + File.separator + senderUserName);
                if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                    System.err.println("Failed to create downloads directory.");
                    return;
                }

                File file = new File(downloadsDir, fileName);
                try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("Received file: " + fileName + " from " + senderUserName);
                String[] parts = fileName.split("\\.");
                String fileType = parts[parts.length - 1];
                if (fileType.equals("jpeg") || fileType.equals("jpg") || fileType.equals("png")) {
                    new clientUI("Downloads/" + senderUserName + "/" + fileName);
                } else if (fileType.equals("mp4")) {
                    String path = client.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                    int spliceIndex = path.length() - 15;
                    if (spliceIndex > 0) {
                        path = path.substring(0, spliceIndex);
                        clientUI.displayVideo(path + "Downloads/" + senderUserName + "/" + fileName);
                    } else {
                        System.out.println("Error getting file path!");
                    }
                } else {
                    System.out.println("File recieved is not supported to be displayed!");
                    System.out.println("Supported file types include: jpg, jpeg, png, mp4");
                }
            } catch (IOException e) {
                System.err.println("Error receiving file: " + e.getMessage());
            }
        } else if ("n".equals(response)) {
            writer.println("/fileResponse " + senderUserName + " decline " + fileName);
            System.out.println("P2P file transfer declined.");
        } else {
            System.out.println("Invalid response. Please type 'y' or 'n'.");
        }

    }

    private static ByteArrayOutputStream encodeVideo(String filePath, long timestamp) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(filePath)) {
            grabber.start();
            grabber.setTimestamp(timestamp - 10000000);

            // Intermediate ByteArrayOutputStream
            ByteArrayOutputStream intermediateStream = new ByteArrayOutputStream();

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(intermediateStream, grabber.getImageWidth(),
                    grabber.getImageHeight())) {
                // Uncomment the following line to show the log
                // FFmpegLogCallback.set();

                // need to be further optimized
                recorder.setFormat("mpegts");
                recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setVideoOption("preset", "ultrafast");
                recorder.setVideoOption("tune", "zerolatency");
                recorder.setVideoOption("crf", "35"); // compression vs quality
                recorder.setVideoOption("c:v", "h264_qsv"); // hardware acceleration

                recorder.setImageWidth(grabber.getImageWidth());
                recorder.setImageHeight(grabber.getImageHeight());
                recorder.setVideoBitrate(grabber.getVideoBitrate());
                recorder.setFrameRate(grabber.getFrameRate());

                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setAudioBitrate(grabber.getAudioBitrate());
                recorder.setSampleRate(44100); // this is the default sampleRate
                recorder.setAudioChannels(2); // keep it to stereo
                recorder.setAudioQuality(3);
                recorder.setCloseOutputStream(true);

                recorder.setOption("threads", "auto"); // I believe this helps encode speed

                recorder.start();

                Frame frame;
                while ((frame = grabber.grabFrame()) != null) {
                    recorder.record(frame);
                    // System.out.println(timestamp + "timestamp < grab timestamp" +
                    // grabber.getTimestamp());
                    if (grabber.getTimestamp() > timestamp) {
                        recorder.stop();
                        recorder.release();
                        grabber.stop();
                        grabber.release();
                        return intermediateStream;
                    }
                }
                recorder.stop();
                recorder.release();
                grabber.stop();
                grabber.release();
                return intermediateStream;
            } catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
                e.printStackTrace();
                return null;
            }
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int[] getTimeAndFrameRate(String filepath, long segmentsize) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(filepath)) {
            grabber.start();
            long time = grabber.getLengthInTime();
            int frameRate = (int) Math.ceil(grabber.getFrameRate());
            grabber.stop();
            grabber.release();
            int segments = (int) Math.ceil((double) time / segmentsize);
            return new int[] { segments, frameRate };
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }



    public static byte[] convertShortArrayToByteArray(short[] shortArray) {
        byte[] byteArray = new byte[shortArray.length * 2];
        for (int i = 0; i < shortArray.length; i++) {
            byteArray[i * 2] = (byte) (shortArray[i] & 0xFF);
            byteArray[i * 2 + 1] = (byte) ((shortArray[i] >> 8) & 0xFF);
        }
        return byteArray;
    }

    // just testing sending first before 2 way
    private static void callP2PVideo(String targetUser, PrintWriter writer) {
        stopSignal = false;
        ServerSocket serverSocket = null;
        try {
            System.out.println("Attempting to start a bi-directional video call with " + targetUser);
            serverSocket = new ServerSocket(0); // Initialize serverSocket
            int localPort = serverSocket.getLocalPort();
            String localAddress = InetAddress.getLocalHost().getHostAddress();
            writer.println("/call " + targetUser + " " + localPort + " " + localAddress);
            serverSocket.setSoTimeout(20000);

            try (Socket p2pSocket = serverSocket.accept()) { // This will be automatically closed
                System.out.println("Call accepted. Starting video stream.");

                // Threads for video streaming
                Thread sendThread = new Thread(() -> {
                    try {
                        System.out.println("Starting to send video.");
                        streamWebcam(new CanvasFrame("My Camera"), p2pSocket.getOutputStream());
                    } catch (IOException e) {
                        System.err.println("Failed to send video: " + e.getMessage());
                    }
                });

                Thread receiveThread = new Thread(() -> {
                    try {
                        System.out.println("Starting to receive video.");
                        receiveVideoStream(p2pSocket.getInputStream(), new CanvasFrame("Incoming Video"), 29);
                    } catch (IOException e) {
                        System.err.println("Failed to receive video: " + e.getMessage());
                    }
                });

                sendThread.start();
                receiveThread.start();

                sendThread.join();
                receiveThread.join();

                System.out.println("Video call ended.");
            }catch (SocketTimeoutException e) {
                System.err.println("Call attempt timed out. No connection was established.");
                // Handle the timeout case, such as notifying users or retrying
            }
        } catch (Exception e) {
            System.err.println("Error in callP2PVideo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("ServerSocket closed successfully.");
                } catch (IOException e) {
                    System.err.println("Error closing ServerSocket: " + e.getMessage());
                }
            }
        }
    }


    // basic implementation to attempt to get it to work
    private static void streamWebcam(CanvasFrame canvasFrame, OutputStream outputStream) {
        canvasFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        try (OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0)) {
            grabber.start();
            ByteArrayOutputStream segmentStream = new ByteArrayOutputStream();
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(segmentStream, grabber.getImageWidth(), grabber.getImageHeight())) {
                configureRecorder(recorder, grabber.getSampleRate(), grabber.getAudioChannels());


                recorder.start();



                long lastSegmentTime = System.currentTimeMillis();

                while (!stopSignal) {
                    if (!canvasFrame.isVisible()) {
                        System.out.println("Window closed by user. Closing connection");
                        outputStream.close();
                        stopSignal = true;
                        break;
                    }

                    Frame frame = grabber.grabFrame();
                    if (frame != null && canvasFrame.isVisible()) {
                        recorder.record(frame);
                        canvasFrame.showImage(frame);
                    }

                    if (System.currentTimeMillis() - lastSegmentTime >= 2000) {
                        byte[] segmentBytes = segmentStream.toByteArray();
                        if (segmentBytes.length > 0) {
                            sendSegment(outputStream, segmentBytes);
                            segmentStream.reset();
                        }
                        lastSegmentTime = System.currentTimeMillis();
                    }
                }

                // Send any remaining bytes before stopping
                byte[] remainingBytes = segmentStream.toByteArray();
                if (remainingBytes.length > 0) {
                    sendSegment(outputStream, remainingBytes);
                }

                sendSegment(outputStream, null); // Send end-of-stream marker
            }
        } catch (Exception e) {
            System.err.println("Error during webcam streaming: " + e.getMessage());
            stopSignal = true; // Indicate to stop all streaming-related operations
            e.printStackTrace();
        } finally {
            if (canvasFrame.isVisible()) {
                canvasFrame.dispose();
            }
        }
    }

    private static void sendSegment(OutputStream outputStream, byte[] segmentBytes) throws IOException {
        if (segmentBytes == null) {
            outputStream.write(ByteBuffer.allocate(4).putInt(0).array());
        } else {
            outputStream.write(ByteBuffer.allocate(4).putInt(segmentBytes.length).array());
            outputStream.write(segmentBytes);
        }
        outputStream.flush();
    }
    private static void configureRecorder(FFmpegFrameRecorder recorder, int sampleRate, int audioChannels) {
        recorder.setInterleaved(true);
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "28");
        recorder.setVideoBitrate(2000000);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mpegts");
        recorder.setFrameRate(29);
        recorder.setGopSize(30);

        recorder.setAudioOption("crf", "20");
        recorder.setAudioQuality(0);
        recorder.setAudioBitrate(0);
        recorder.setSampleRate(sampleRate > 0 ? sampleRate : 44100); // Use grabber's sample rate if available
        recorder.setAudioChannels(audioChannels > 0 ? audioChannels : 2);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setOption("threads", "auto");
    }

    private static void receiveVideoStream(InputStream inputStream, CanvasFrame canvas, int framerate) {
        canvas.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        System.out.println("in receiveVideo");

        // Initialize grabber outside the loop for better performance
        FFmpegFrameGrabber grabber = null;

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(inputStream))) {
            System.out.println("in receiveVideo2");

            while (!stopSignal) {
                // Read the length of the next video segment
                int segmentLength;
                try {
                    segmentLength = dis.readInt(); // This might be a blocking call, but we know data is available
                } catch (IOException e) {
                    System.err.println("Error reading segment length: " + e.getMessage());
                    break; // Exit the loop on error
                }

                if (segmentLength == 0) {
                    System.out.println("Received end-of-stream marker. Exiting receiveVideoStream loop.");
                    break; // End-of-stream marker
                }

                System.out.println("Received segment of size: " + segmentLength); // Debug log
                byte[] segmentData = new byte[segmentLength];
                try {
                    dis.readFully(segmentData); // Read the complete segment
                } catch (IOException e) {
                    System.err.println("Error reading segment data: " + e.getMessage());
                    break; // Exit the loop on error
                }

                if (grabber == null) {
                    grabber = new FFmpegFrameGrabber(new ByteArrayInputStream(segmentData));
                    grabber.setFormat("mpegts");
                    grabber.setOption("hwaccel", "qsv"); // hardware acceleration
                    grabber.setFrameRate(framerate);
                    grabber.setOption("threads", "auto"); // I believe this helps decode speed
                    grabber.start();
                } else {
                    grabber.stop(); // Stop the grabber
                    grabber.close(); // Close the grabber
                    grabber = new FFmpegFrameGrabber(new ByteArrayInputStream(segmentData)); // Reinitialize the grabber
                    grabber.setFormat("mpegts");
                    grabber.setOption("hwaccel", "qsv"); // hardware acceleration
                    grabber.setFrameRate(framerate);
                    grabber.setOption("threads", "auto"); // I believe this helps decode speed
                    grabber.start();
                }

                long frameInterval = 1000 / framerate; // Frame interval in milliseconds
                long nextFrameTime = System.currentTimeMillis() + frameInterval;

                Frame frame;
                while (!stopSignal && (frame = grabber.grabFrame()) != null) {
                    if (frame.image != null) {
                        if (!canvas.isVisible()) {
                            System.out.println("Window closed by user. Closing connection");
                            inputStream.close();
                            stopSignal = true;
                            break;
                        }
                        canvas.showImage(frame);

                        long currentTime = System.currentTimeMillis();
                        long waitTime = nextFrameTime - currentTime;
                        if (waitTime > 0) {
                            Thread.sleep(waitTime);
                        }
                        nextFrameTime += frameInterval;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error in receiveVideoStream: " + e.getMessage());
            e.printStackTrace();
            stopSignal = true;
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted while waiting: " + e.getMessage());
            stopSignal = true; // Ensure we stop on error
        } finally {
            // Close grabber outside the loop
            if (grabber != null) {
                try {
                    grabber.stop();
                } catch (FrameGrabber.Exception e) {
                    System.err.println("Error stopping grabber: " + e.getMessage());
                }
            }
            if (canvas.isVisible()) {
                canvas.dispose();
            }
            System.out.println("Exiting receiveVideoStream method.");
        }
    }



    private static void receiveCallP2PVideo(String senderUserName, String senderAddress, int senderPort, PrintWriter writer, String response) {
        if ("y".equals(response)) {
            writer.println("/callResponse " + senderUserName + " accept");

            try {
                Socket socket = new Socket(senderAddress, senderPort);
                System.out.println("CONNECTED");
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                socket.setPerformancePreferences(0, 1, 2);
                int framerate = 29;
                socket.setSoTimeout(150000);


                Thread sendThread = new Thread(() -> {
                    try {
                        System.out.println("Starting to send video.");
                        streamWebcam(new CanvasFrame("My Camera"), socket.getOutputStream());
                    } catch (IOException e) {
                        System.err.println("Failed to send video: " + e.getMessage());
                    }
                });



                Thread receiveThread = new Thread(() -> {
                    try {
                        System.out.println("Starting to receive video.");
                        receiveVideoStream(socket.getInputStream(), new CanvasFrame("Incoming Video"), framerate);
                    } catch (IOException e) {
                        System.err.println("Failed to receive video: " + e.getMessage());
                    }
                });

                sendThread.start();
                receiveThread.start();

                sendThread.join();
                receiveThread.join();

            } catch (IOException | InterruptedException e) {
                System.err.println("Error setting up bi-directional stream: " + e.getMessage());
                e.printStackTrace(); // Print the stack trace to help debug
            }
        } else {
            writer.println("/callResponse " + senderUserName + " decline ");
            System.out.println("P2P stream declined");
            stopSignal = true;
        }
    }






    private static void streamP2PVideo(String targetUser, String filePath, PrintWriter writer) {
        System.out.println("Waiting for Response:");
        stopSignal = false;  // initialize the stop signal
        long segmentSize = 10000000;
        int[] vidInfo = getTimeAndFrameRate(filePath, segmentSize);
        int segments = vidInfo[0];
        int frameRate = vidInfo[1];

        java.util.Queue<ByteArrayOutputStream> queue = new LinkedList<>();


        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int localPort = serverSocket.getLocalPort();
            String address = InetAddress.getLocalHost().getHostAddress();
            writer.println("/stream " + targetUser + " " + new File(filePath).getName() + " " + localPort + " " + segments + " " + frameRate + " " + address);

            try (Socket p2pSocket = serverSocket.accept()) {

                Thread encodingThread = new Thread(() -> {
                    for (int i = 1; i <= segments && !stopSignal; i++) {
                        ByteArrayOutputStream encodedSegment = encodeVideo(filePath, segmentSize * i);
                        synchronized (queue) {
                            if (stopSignal) break;
                            queue.add(encodedSegment);
                            queue.notify();
                        }
                    }
                    synchronized (queue) {
                        queue.notifyAll(); // Ensure to notify in case the thread exits early due to stop signal
                    }
                });
                encodingThread.start();

                p2pSocket.setTcpNoDelay(true);
                p2pSocket.setPerformancePreferences(0, 1, 2);
                OutputStream outputStream = p2pSocket.getOutputStream();

                while (!stopSignal) {
                    ByteArrayOutputStream segment;
                    synchronized (queue) {
                        while (queue.isEmpty() && !stopSignal) {
                            queue.wait(); // Wait for segments to be available or stop signal
                        }
                        segment = queue.poll(); // pull segment from queue
                    }

                    if (segment == null || stopSignal) break;

                    if (segment.size() == 0) { // End of stream marker reached
                        byte[] endOfStreamMarker = ByteBuffer.allocate(4).putInt(0).array();
                        outputStream.write(endOfStreamMarker);
                        outputStream.flush();
                        System.out.println("All segments sent. End of stream marker sent. Closing socket.");
                        break;
                    }

                    byte[] segmentBytes = segment.toByteArray();
                    try {
                        outputStream.write(ByteBuffer.allocate(4).putInt(segmentBytes.length).array()); // Write segment size
                        outputStream.write(segmentBytes); // Write segment data
                        outputStream.flush();
                    } catch (IOException e) {
                        System.out.println("Receiver has closed the connection. Stopping the sending process.");
                        stopSignal = true; // Signal to stop all processes
                        break;
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Sending thread was interrupted.");
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        } catch (IOException e) {
            System.out.println("Failed to stream video: " + e.getMessage());
        } finally {
            stopSignal = true; // Ensure all processes stop
            // Join the encoding thread to ensure it has stopped before exiting
        }
        System.out.println("Stream session ended.");
    }



    private static void receiveP2PVideo(String senderUserName, String senderAddress, int senderPort, String fileName, PrintWriter writer, String response, int frameRate) {
        if ("y".equals(response)) {
            writer.println("/streamResponse " + senderUserName + " accept " + fileName);

            CanvasFrame canvas = new CanvasFrame("Video");
            canvas.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);

            try (Socket socket = new Socket(senderAddress, senderPort);
                 BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {

                System.out.println("Connection established...");
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true); // checks if socket is alive
                socket.setPerformancePreferences(0, 1, 2); // priority for socket

                socket.setSoTimeout(10000); // set timeout

                while (true) {
                    try {
                        if (!canvas.isVisible()) {
                            System.out.println("Window closed by user. Closing connection");
                            socket.close(); // close the socket when window is closed
                            break;
                        }
                        // Check if there is data available to read
                        if (in.available() < 4) {
                            // No data, wait
                            continue;
                        }

                        byte[] sizeBytes = readBytes(in, 4);
                        if (sizeBytes.length == 0) {
                            System.out.println("Unexpected end of stream");
                            break;
                        }
                        int segmentSize = ByteBuffer.wrap(sizeBytes).getInt(); // Read segment size

                        if (segmentSize == 0) {
                            // End of stream marker received, exit loop to close connection
                            System.out.println("End of stream marker. Closing connection");
                            break;
                        }

                        byte[] segmentData = readBytes(in, segmentSize); // Read segment data
                        ByteArrayInputStream segmentStream = new ByteArrayInputStream(segmentData);
                        decodeAndDisplay(new BufferedInputStream(segmentStream), canvas, frameRate);

                    } catch (SocketTimeoutException e) {
                        System.out.println("Socket timeout. Continuing to wait for data");
                    }
                }
                System.out.println("Video streamed: " + fileName + " from " + senderUserName);
            } catch (IOException e) {
                System.err.println("Error receiving stream: " + e.getMessage());
            }
        } else if ("n".equals(response)) {
            writer.println("/streamResponse " + senderUserName + " decline " + fileName);
            System.out.println("P2P stream declined");
        } else {
            System.out.println("Invalid response. Please type 'y' or 'n'");
        }
    }

    private static byte[] readBytes(InputStream inputStream, int numBytes) throws IOException {
        byte[] bytes = new byte[numBytes];
        int totalBytesRead = 0;
        int bytesRead;

        // Continue reading until all bytes are read or end of stream
        while (totalBytesRead < numBytes) {
            bytesRead = inputStream.read(bytes, totalBytesRead, numBytes - totalBytesRead);

            // If no more bytes, break
            if (bytesRead < 0) {
                System.out.println("Unexpected end of stream");
                break;
            }
            totalBytesRead += bytesRead;
        }

        if (totalBytesRead != numBytes) {
            System.out.println("Expected " + numBytes + " bytes but read only " + totalBytesRead + " bytes");
            // Resize the byte array to the number of bytes actually read
            byte[] resizedBytes = new byte[totalBytesRead];
            System.arraycopy(bytes, 0, resizedBytes, 0, totalBytesRead);
            return resizedBytes;
        }
        return bytes;
    }

    private static void decodeAndDisplay(BufferedInputStream in, CanvasFrame canvas, int framerate) {
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(in);
            grabber.setFormat("mpegts");
            grabber.setOption("hwaccel", "qsv"); // hardware acceleration
            grabber.setFrameRate(framerate);
            grabber.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            grabber.setOption("threads", "auto"); // I believe this helps decode speed
            grabber.start();

            AudioFormat audioFormat = new AudioFormat(grabber.getSampleRate(), 16, grabber.getAudioChannels(), true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);
            int bufferSize = 8192; // Adjust as needed
            audioLine.open(audioFormat, bufferSize);
            audioLine.start();

            long frameInterval = 1000 / framerate; // framerate in milliseconds
            long nextFrameTime = System.currentTimeMillis() + frameInterval; // Initialize next frame display time

            Frame frame;
            while ((frame = grabber.grabFrame()) != null) {
                if (frame.samples != null) {
                    processAudio(frame, audioLine);
                }
                if (frame.image != null) {
                    canvas.showImage(frame);

                    // Calculate the delay until the next frame.
                    long currentTime = System.currentTimeMillis();
                    long waitTime = nextFrameTime - currentTime;

                    if (waitTime > 0) {
                        Thread.sleep(waitTime);
                    }

                    nextFrameTime += frameInterval;
                }

            }
            audioLine.drain();
            audioLine.stop();
            audioLine.close();
            grabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void processAudio(Frame frame, SourceDataLine audioLine) {
        // Implementation for processing and playing back audio frames
        Buffer[] samples = frame.samples;
        if (samples[0] instanceof ShortBuffer) {
            ShortBuffer shortBuffer = (ShortBuffer) samples[0];
            shortBuffer.rewind();
            short[] audioData = new short[shortBuffer.remaining()];
            shortBuffer.get(audioData);
            byte[] audioBytes = convertShortArrayToByteArray(audioData);
            audioLine.write(audioBytes, 0, audioBytes.length);
        }
    }

}
