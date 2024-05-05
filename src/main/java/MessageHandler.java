import java.io.PrintWriter;
import java.util.*;

class MessageHandler implements Runnable {
    private Map<String, PrintWriter> clientMap;
    private final Map<String, ArrayList<String>> roomMap;
    private final Queue<String[]> messageQueue;
    // packet = [message, roomName]

    MessageHandler() {
        this.messageQueue = new LinkedList<>();
        // [key] : [value]
        // [user] : [printwriter]
        this.clientMap = new HashMap<>();
        // [key] : [value]
        // [roomname] : [user1, user2, ...]
        this.roomMap = new HashMap<>();
    }

    @Override
    public void run() {
        String[] message;
        System.out.println("message handler has been created");
        while(true) {
            try {
                // handles client messages in messageQueue
                synchronized (messageQueue) {
                    if (messageQueue.isEmpty()) {
                        messageQueue.wait();
                    }
                    message = messageQueue.remove();
                }
                // responses to client messages
                switch((message[1].split(" ")[0])) {
                    case "/message":{
                        break;
                    }
                    case "/create": {
                        createRoom(message[0], message[1]);
                        break;
                    }
                    case "/room": {
                        userMessageToRoom(message[0], message[1]);
                        break;
                    }
                    case "/join": {
                        addUserToRoom(message[0], message[1]);
                        break;
                    }
                    case "/leave": {
                        leaveRoom(message[0], message[1]);
                        break;
                    }
                    case "/members": {
                        printRoomMembers(message[0], message[1]);
                        break;
                    }
                    case "/rooms": {
                        printRooms(message[0], message[1]);
                        break;
                    }
                    case "/delete": {
                        deleteRoom(message[0], message[1]);
                        break;
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
//        } finally {
//            try {
//                roomMap.remove(roomName);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
            }
        }
    }

    // adds client message to messageQueue
    public void message(String sender, String message){
        String[] packet = {sender, message};
        synchronized (messageQueue){
            messageQueue.add(packet);
            messageQueue.notify();
        }
    }
    // updates MessageHandler clientMap - used in Server
    public void setClientMap(Map<String,PrintWriter> newMap)
    {
        synchronized (clientMap){
            this.clientMap = newMap;
        }
    }
    private void messageToUser(String receiver, String message) {
        PrintWriter writer = clientMap.get(receiver);
        writer.println(message);
    }
    // helper method: adds client to roomMap values
    private void addUserToRoomMap(String userName, String roomName){
        ArrayList<String> roomMembers = roomMap.getOrDefault(roomName, new ArrayList<>());
        if(roomMembers.contains(userName)){
            messageToUser(userName,"You've already subscribed to " + roomName);
        } else {
            roomMembers.add(userName);
            synchronized (roomMap) {
                roomMap.put(roomName, roomMembers);
            }
            sendMessageToRoom(roomName, userName + " has joined the room: " + roomName);
        }
    }
    // removes client from subscribed clients to the chat room
    private void leaveRoom(String sender, String message){
        String[] parts = message.split(" ", 3);
        String roomName = parts[1];
        if (parts.length == 2) {
            if(roomMap.containsKey(roomName)) {
                sendMessageToRoom(roomName, sender + " has left the room: " + roomName);
                removeUserFromRoomMap(roomName, sender);
            } else {
                messageToUser(sender,roomName + " does not exist");
            }
        } else {
            messageToUser(sender,"Usage: /leave <roomname>");
        }
    }
    // helper method: removes client from roomMap values
    private void removeUserFromRoomMap(String roomName, String userName) {
        if (roomMap.containsKey(roomName)) {
            synchronized (roomMap) {
                roomMap.get(roomName).remove(userName);
//                if (roomMap.get(roomName).isEmpty()) {
//                    roomMap.remove(roomName);
//                }
            }
        }
    }
    // creates a chat room clients can join
    private void createRoom(String sender, String message){
        String[] parts = message.split(" ", 2);
        if (parts.length == 2) {
            String roomName = parts[1];
            if(roomMap.containsKey(roomName)){
                messageToUser(sender,roomName + " already exists");
            } else {
                synchronized (roomMap){
                    roomMap.put(roomName, new ArrayList<String>());
                }
                messageToUser(sender, roomName + " has been created");
                addUserToRoomMap(sender, roomName);
//                sendMessageToRoom(roomName, sender + " has joined room: " + roomName);
            }
        } else {
            messageToUser(sender,"Usage: /create <roomname>");
        }
    }
    // deletes the chat room
    private void deleteRoom(String sender, String message){
        String[] parts = message.split(" ", 3);
        String roomName = parts[1];
        if (parts.length == 2) {
            if(roomMap.containsKey(roomName)){
                sendMessageToRoom(roomName, roomName + " has been deleted");
                synchronized (roomMap){
                    roomMap.remove(roomName);
                }
            } else {
                messageToUser(sender,roomName + " does not exist");
            }
        } else {
            messageToUser(sender,"Usage: /delete <roomname>");
        }
    }
    // sends client message to all subscribers of a room
    private void userMessageToRoom(String sender, String message){
        String[] parts = message.split(" ", 3);
        String roomName = parts[1];
        if (parts.length == 3) {
            if(roomMap.containsKey(roomName)) {
                String messageLine = sender + " (" + roomName + "):" + " " + parts[2];
                sendMessageToRoom(roomName, messageLine);
            } else {
                messageToUser(sender,roomName + " does not exist");
            }
        }
        else {
            messageToUser(sender,"Usage: /room <roomname> <message>");
        }
    }
    // sends message to all subscribers of a room
    private void sendMessageToRoom(String roomName, String message) {
        ArrayList<String> roomMembers = roomMap.get(roomName);
        if (roomMembers != null) {
            System.out.println("members: " + roomMembers);
            for (String member : roomMembers) {
                System.out.println(roomName + ": sending to room member: " + member + ", " + message);
                messageToUser(member, message);
            }
        }
    }
    // subscribes the client to the room
    private void addUserToRoom(String sender, String message){
        String[] parts = message.split(" ", 3);
        String roomName = parts[1];
        if (parts.length == 2) {
            if(roomMap.containsKey(roomName)) {
                addUserToRoomMap(sender, roomName);
            } else {
                messageToUser(sender,roomName + " does not exist");
            }
        }
        else {
            messageToUser(sender,"Usage: /join <roomname>");
        }
    }
    // sends client the subscribers of the chat room
    private void printRoomMembers(String sender, String message){
        String[] parts = message.split(" ", 2);
        String roomName = parts[1];
        if (parts.length == 2) {
            if(roomMap.containsKey(roomName)) {
                messageToUser(sender, String.valueOf(roomMap.get(roomName)));
            } else {
                messageToUser(sender,roomName + " does not exist");
            }
        } else {
            messageToUser(sender,"Usage: /members <roomname>");
        }
    }
    // sends client the existing chat rooms
    private void printRooms(String sender, String message) {
        String[] parts = message.split(" ");
        if (parts.length == 1) {
            messageToUser(sender, String.valueOf(roomMap.keySet()));
        } else {
            messageToUser(sender,"Usage: /rooms");
        }
    }
}
