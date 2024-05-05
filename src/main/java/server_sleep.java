import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class server_sleep {
    static final int PORT = 9000;
    // public static final Map<String, PrintWriter> clientMap = new ConcurrentHashMap<>();
    // public static final Map<String, Boolean> onlineStatus = new ConcurrentHashMap<>();
    // private static final Map<String, Socket> clientSockets = new ConcurrentHashMap<>();
    // public static ConcurrentHashMap<String, HashMap<String, String>> clientProfiles = new ConcurrentHashMap<>();
    // protected static final ExecutorService executorService = Executors.newFixedThreadPool(100);
    // private static final Lock fileLock = new ReentrantLock(); // Lock for file writing
    // private static final Map<String, lobby> lobbyMap = new ConcurrentHashMap<>();
    // protected static final Map<String, String> UserLobbyMap = new ConcurrentHashMap<>();
    // private static final MessageHandler messageHandler = new MessageHandler();

    public static final Map<String, PrintWriter> clientMap = new HashMap<>();
    public static final Map<String, Boolean> onlineStatus = new HashMap<>();
    private static final Map<String, Socket> clientSockets = new HashMap<>();
    public static HashMap<String, HashMap<String, String>> clientProfiles = new HashMap<>();
    protected static final ExecutorService executorService = Executors.newFixedThreadPool(100);
    private static final Lock fileLock = new ReentrantLock(); // Lock for file writing
    private static final Map<String, lobby> lobbyMap = new HashMap<>();
    protected static final Map<String, String> UserLobbyMap = new HashMap<>();
    private static final MessageHandler messageHandler = new MessageHandler();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started and listening on port " + PORT);
            executorService.execute(messageHandler);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                String userName = reader.readLine();

                if (userName != null && !userName.trim().isEmpty()) {
                    synchronized (clientMap) {
                        clientMap.put(userName, writer);
                        messageHandler.setClientMap(clientMap);
                    }
                    synchronized (onlineStatus) {
                        onlineStatus.put(userName, true);
                    }
                    synchronized (clientProfiles) {
                        clientProfiles.put(userName, new HashMap<String, String>());
                    }
                    clientSockets.put(userName, clientSocket);
                    executorService.execute(new ClientHandler(clientSocket, userName));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final String userName;
        private HashMap<String, String> profile;

        ClientHandler(Socket clientSocket, String userName) {
            this.clientSocket = clientSocket;
            this.userName = userName;
            // Add keys and empty values to profile HashMap:
            this.profile = new HashMap<String, String>();
            profile.put("Image", " ");
            profile.put("Nickname", " ");
            profile.put("AboutMe", " ");
            profile.put("StatusMessage", " ");
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                String message;
                synchronized (ClientHandler.class) {
                    clientProfiles.put(userName, profile);
                }

                writer.println("Please select on option (there are infinite)");

                while ((message = reader.readLine()) != null) {
                    synchronized (UserLobbyMap) {
                        synchronized (clientMap) {
                            synchronized (onlineStatus) {
                                if (message.startsWith("/")) {
                                    switch (message.split(" ")[0]) {
                                        case "/rooms":
                                        case "/create":
                                        case "/join":
                                        case "/leave":
                                        case "/members":
                                        case "/delete":
                                        case "/room": {
                                            System.out.println("sending to messagehandler");
                                            messageHandler.message(userName, message);
                                            break;
                                        }
                                        case "/status":
                                            onlineStatus.forEach(
                                                    (key, value) -> writer
                                                            .println(key + " " + (value ? "online" : "offline")));
                                            break;

                                        case "/logout":
                                            onlineStatus.put(userName, false);
                                            clientMap.remove(userName);
                                            writer.println("You have logged out.");
                                            clientSocket.close();
                                            break;

                                        case "/message":
                                            handleMessage(message, writer);
                                            break;

                                        case "/Eprofile":
                                            String clientChange = reader.readLine(); // Read target username
                                            String profileElementChange = reader.readLine(); // Read profile element to
                                            // edit
                                            String actualChange = reader.readLine(); // Read new value for the profile
                                            // element

                                            synchronized (ClientHandler.class) {
                                                // Check if the target user exists
                                                if (clientProfiles.containsKey(clientChange)) {
                                                    Thread.sleep(3000); // Sleep 3 seconds for demo's sake
                                                    // Get the target user's profile
                                                    HashMap<String, String> targetProfile = clientProfiles
                                                            .get(clientChange);
                                                    // Update the specified profile element with the new value
                                                    targetProfile.put(profileElementChange, actualChange);
                                                    writer.println("Profile element '" + profileElementChange
                                                            + "' successfully edited for " + clientChange);
                                                } else {
                                                    writer.println("User '" + clientChange + "' not found.");
                                                }
                                            }
                                            break;

                                        case "/Vprofile":
                                            String profileUsername = reader.readLine(); // Read target username

                                            synchronized (ClientHandler.class) {
                                                // Checks if user exists
                                                if (clientProfiles.containsKey(profileUsername)) {
                                                    Thread.sleep(3000); // Sleep 3 seconds for demo's sake
                                                    // Loop through each client
                                                    for (Map.Entry<String, HashMap<String, String>> client : clientProfiles
                                                            .entrySet()) {
                                                        String clientUsername = client.getKey();
                                                        // for each client in the clientProfiles map, check if the
                                                        // username
                                                        // matches the desired one
                                                        if (profileUsername.equals(clientUsername)) {
                                                            // if it does, print the username followed by the profile
                                                            // info
                                                            // from
                                                            // within it
                                                            writer.println(profileUsername + ":\n");
                                                            for (Map.Entry<String, String> profileEntry : client
                                                                    .getValue()
                                                                    .entrySet()) {
                                                                String key = profileEntry.getKey();
                                                                String value = profileEntry.getValue();
                                                                writer.println(key + ": " + value);
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    writer.println(
                                                            "Something went wrong, check your spelling and try again.");
                                                }
                                            }
                                            writer.println("\n");
                                            break;

                                        case "/newLobby":

                                            String[] parts4 = message.split(" ");
                                            if (parts4.length > 1) {

                                                String LobbyName = parts4[1];
                                                if (lobbyMap.containsKey(LobbyName)) {
                                                    // Lobby Already Exists
                                                    System.out.println("lobby exists");
                                                    writer.println("Lobby " + LobbyName + " already exists");
                                                } else {
                                                    // Lobby Exists
                                                    System.out.println("lobby doesn't exist");
                                                    lobbyMap.put(LobbyName, new lobby(LobbyName));
                                                    if (lobbyMap.get(LobbyName).addUser(userName)) {
                                                        UserLobbyMap.put(userName, LobbyName);
                                                        writer.println("Lobby " + LobbyName + " created and joined");
                                                        System.out.println("user should be added");
                                                    }

                                                }
                                            }
                                            break;

                                        case "/joinLobby":
                                            String[] parts5 = message.split(" ");
                                            if (parts5.length > 1) {
                                                String LobbyName = parts5[1];
                                                if (lobbyMap.containsKey(LobbyName)) {
                                                    // Lobby Already Exists
                                                    if (lobbyMap.get(LobbyName).addUser(userName)) {
                                                        UserLobbyMap.put(userName, LobbyName);
                                                    } else {
                                                        writer.println("Lobby is Locked");
                                                    }
                                                } else {
                                                    writer.println("Lobby doesn't exist");
                                                }
                                            }

                                            break;

                                        case "/file":
                                            String[] parts2 = message.split(" ");
                                            if (parts2.length == 5) {
                                                String targetUser = parts2[1];
                                                String fileName = parts2[2];
                                                int port = Integer.parseInt(parts2[3]);
                                                String address = parts2[4];
                                                initiateP2PTransfer(targetUser, fileName, port, userName, address);
                                            } else {
                                                writer.println("incorrect ussage /file");
                                            }
                                            break;

                                        case "/fileResponse":
                                            // Example: /fileResponse senderUser accept fileName
                                            String[] responseParts = message.split(" ");
                                            if (responseParts.length == 4) {
                                                String senderUser = responseParts[1];
                                                String response = responseParts[2]; // "accept" or "decline"
                                                String fileName = responseParts[3];
                                                PrintWriter senderWriter = clientMap.get(senderUser);
                                                if (senderWriter != null) {
                                                    senderWriter.println("/fileResponse " + response + " " + fileName);
                                                }
                                            }
                                            break;

                                        case "/stream":
                                            String[] parts3 = message.split(" ");
                                            System.out.println(parts3.length);
                                            if (parts3.length == 7) {
                                                String targetUser = parts3[1];
                                                String fileName = parts3[2];
                                                int port = Integer.parseInt(parts3[3]);
                                                int segments = Integer.parseInt(parts3[4]);
                                                int frameRate = Integer.parseInt(parts3[5]);
                                                String address = parts3[6];
                                                initiateP2PStream(targetUser, fileName, port, userName, segments,
                                                        frameRate,
                                                        address);
                                            } else {
                                                writer.println("incorrect ussage /stream");
                                            }
                                            break;

                                        case "/streamResponse":
                                            // Example: /streamResponse senderUser accept streamName
                                            String[] responseArr = message.split(" ");
                                            if (responseArr.length == 4) {
                                                String senderUser = responseArr[1];
                                                String response = responseArr[2]; // "accept" or "decline"
                                                String streamName = responseArr[3];
                                                PrintWriter senderWriter = clientMap.get(senderUser);
                                                if (senderWriter != null) {
                                                    senderWriter
                                                            .println("/streamResponse " + response + " " + streamName);
                                                }
                                            }
                                            break;

                                        case "/call":
                                            String[] parts7 = message.split(" ");
                                            System.out.println(parts7.length);
                                            if (parts7.length == 4) {
                                                String targetUser = parts7[1];
                                                int port = Integer.parseInt(parts7[2]);
                                                String address = parts7[3];
                                                initiateP2PCall(targetUser, port, userName, address);
                                            } else {
                                                writer.println("incorrect ussage /call");
                                            }
                                            break;

                                        case "/callResponse":
                                            String[] responseArr2 = message.split(" ");
                                            if (responseArr2.length == 4) {
                                                String senderUser = responseArr2[1];
                                                String response = responseArr2[2]; // "accept" or "decline"
                                                String streamName = responseArr2[3];
                                                PrintWriter senderWriter = clientMap.get(senderUser);
                                                if (senderWriter != null) {
                                                    senderWriter
                                                            .println("/callResponse " + response + " " + streamName);
                                                }
                                            }
                                            break;

                                        default:
                                            writer.println("Unknown command.");
                                            break;
                                    }
                                } else {
                                    for (PrintWriter w : clientMap.values()) {
                                        w.println(userName + ": " + message);
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    synchronized (clientMap) {
                        synchronized (onlineStatus) {
                            onlineStatus.put(userName, false);
                            clientMap.remove(userName);
                        }
                    }
                    synchronized (ClientHandler.class) {
                        clientProfiles.remove(userName);
                    }
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        private void handleMessage(String message, PrintWriter writer) {
            String[] parts = message.split(" ");
            if (parts.length >= 3) {
                String targetUser = parts[1];
                String privateMessage = String.join(" ", parts).substring(parts[0].length() + parts[1].length() + 2);
                synchronized (clientMap) {
                    synchronized (onlineStatus) {
                        PrintWriter targetWriter = clientMap.get(targetUser);
                        Boolean status = onlineStatus.getOrDefault(targetUser, true);
                        if (targetWriter != null) {
                            targetWriter.println(userName + " messages: " + privateMessage);
                            writer.println("Sent: " + privateMessage + " to " + targetUser);
                        } else if (!status) {
                            System.out.println(message); // COMMENT OUT WHEN TESTING HJ
                            writeMessageToFile(targetUser, userName + " messages: " + privateMessage);
                            writer.println("Sent: " + privateMessage + " to " + targetUser
                                    + " on the logs since they are offline");
                        } else {
                            writer.println("User not found");
                        }
                    }
                }
            } else {
                writer.println("Usage: /message <username> <message>");
            }
        }

        private void writeMessageToFile(String targetUser, String message) {
            fileLock.lock();
            try (PrintWriter logWriter = new PrintWriter(new FileWriter(targetUser, true))) {
                logWriter.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                fileLock.unlock(); // Unlock after writing to file
            }
        }

        private void initiateP2PTransfer(String targetUser, String fileName, int port, String senderUserName,
                                         String targetAddress) {
            Socket targetSocket = clientSockets.get(targetUser);
            if (targetSocket != null) {
                try {
                    PrintWriter targetWriter = new PrintWriter(targetSocket.getOutputStream(), true);
                    // Notify the sender that the request has been forwarded
                    PrintWriter senderWriter = clientMap.get(senderUserName);
                    // Forward the file transfer request to the target user
                    targetWriter.println(
                            "/waitForP2P " + senderUserName + " " + targetAddress + " " + port + " " + fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                PrintWriter writer = clientMap.get(senderUserName);
                if (writer != null) {
                    writer.println("User " + targetUser + " not found.");
                }
            }
        }

        private void initiateP2PStream(String targetUser, String fileName, int port, String senderUserName,
                                       int segments, int framerate, String targetAddress) {
            Socket targetSocket = clientSockets.get(targetUser);
            if (targetSocket != null) {
                try {
                    PrintWriter targetWriter = new PrintWriter(targetSocket.getOutputStream(), true);
                    // Notify the sender that the request has been forwarded
                    PrintWriter senderWriter = clientMap.get(senderUserName);
                    // Forward the stream request to the target user
                    targetWriter.println("/steamP2P " + senderUserName + " " + targetAddress + " " + port + " "
                            + fileName + " " + segments + " " + framerate);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                PrintWriter writer = clientMap.get(senderUserName);
                if (writer != null) {
                    writer.println("User " + targetUser + " not found.");
                }
            }
        }

        private void initiateP2PCall(String targetUser, int port, String senderUserName, String targetAddress) {
            Socket targetSocket = clientSockets.get(targetUser);
            if (targetSocket != null) {
                try {
                    PrintWriter targetWriter = new PrintWriter(targetSocket.getOutputStream(), true);
                    // Notify the sender that the request has been forwarded
                    PrintWriter senderWriter = clientMap.get(senderUserName);
                    // Forward the stream request to the target user
                    targetWriter.println("/callP2P " + senderUserName + " " + targetAddress + " " + port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                PrintWriter writer = clientMap.get(senderUserName);
                if (writer != null) {
                    writer.println("User " + targetUser + " not found.");
                }
            }
        }
    }
}
