import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class lobby_unsafe extends server_unsafe {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private static String LobbyName = "something went wrong :(";
    private static ArrayList<String> Users = new ArrayList<String>();
    private static final Map<String, String> UserInput = new HashMap<>();
    private static boolean SequentialInput = false;
    private static boolean LobbyLocked = false;

    lobby_unsafe(String Input) {
        LobbyName = Input;
    }

    private static void SendAll(String Msg) {
        for (String User : Users) {
            clientMap.get(User).println(Msg);
        }
    }

    private static void SendUser(String User, String Msg) {
        clientMap.get(User).println(Msg);
    }

    public boolean addUser(String UserName) {
        if (LobbyLocked) {
            return false;
        } else {
            Users.add(UserName);
            UserInput.put(UserName, "none");
            return true;
        }
    }

    public void sendMessage(String UserName, String Message) {
        System.out.println("Lobby: " + LobbyName + ", " + UserName + " sent:[" + Message + "]");
        if (SequentialInput) {
            UserInput.put(UserName, Message);

        } else {
            if (Message.equals("/ping")) {
                SendAll(ANSI_RED + "GAME: " + ANSI_RESET + LobbyName);
            } else if (Message.equals("/exit")) {
                SendUser(UserName, "Leaving the lobby");
                Users.remove(UserName);
                UserLobbyMap.remove(UserName);
            } else if (Message.equals("/lock")) {
                LobbyLocked = true;
                SendAll("Lobby Locked");
            } else if (Message.equals("/unlock")) {
                LobbyLocked = false;
                SendAll("Lobby Unlocked");
            } else if (Message.startsWith("/startGame ")) {
                String[] parts = Message.split(" ");
                if (parts.length > 1) {
                    if (parts[1].equals("Pong")) {
                        executorService.execute(new Pong());
                        SendAll("Pong start");
                        SequentialInput = true;
                    } else if (parts[1].equals("Hangman")) {
                        executorService.execute(new Hangman());
                        SendAll("Hangman start");
                        SequentialInput = true;
                    } else if (parts[1].equals("Apple")) {
                        executorService.execute(new Apple());
                        SequentialInput = true;
                    }

                }
            }
        }
    }

    static class Hangman implements Runnable {
        public void run() {

            ArrayList<Character> Guesses = new ArrayList<Character>();
            ArrayList<Character> CorrectGuesses = new ArrayList<Character>();
            String Solution = "";
            String DisplaySolution = "";
            Map<String, String> UserInputCompare = new HashMap<>();

            SequentialInput = true;
            LobbyLocked = true;

            boolean GameRunning = true;
            int attempts = 5;
            int State = 0;
            int WordSelectionUser = 0;
            char Guess = '-';

            Users.forEach((e) -> {
                UserInput.put(e, "-");
            });
            Users.forEach((e) -> {
                UserInputCompare.put(e, "-");
            });
            // try {
            while (GameRunning) {
                switch (State) {
                    case 0: // Send announcement
                        clientMap.get(Users.get(WordSelectionUser)).println("Please enter a sentence to be guessed:");
                        for (int i = 0; i <= Users.size() - 1; i++) {
                            if (i != WordSelectionUser) {
                                clientMap.get(Users.get(i)).println(
                                        "Please wait until " + Users.get(WordSelectionUser) + " picks a prompt");
                            }
                        }
                        State = 1;
                        break;
                    case 1:
                        if (!UserInput.get(Users.get(WordSelectionUser))
                                .equals(UserInputCompare.get(Users.get(WordSelectionUser)))) {
                            UserInputCompare.put(Users.get(WordSelectionUser),
                                    UserInput.get(Users.get(WordSelectionUser)));
                            if (!UserInputCompare.get(Users.get(WordSelectionUser))
                                    .matches("[ABCDEFGHIJKLMNOPQRSTUVWXYZ ]+")) {
                                clientMap.get(Users.get(WordSelectionUser)).println("Invalid characters");
                            } else {

                                Solution = UserInputCompare.get(Users.get(WordSelectionUser));

                                for (int i = 0; i <= Solution.length() - 1; i++) {
                                    if (Solution.charAt(i) == ' ') {
                                        DisplaySolution = DisplaySolution + " ";
                                    } else {
                                        DisplaySolution = DisplaySolution + "_";
                                    }
                                    DisplaySolution = DisplaySolution + " ";
                                }
                                State = 2;
                                SendAll("Solution: " + DisplaySolution + " | Failed attempts left: " + attempts);
                            }

                        }
                        break;
                    case 2: // Chat/Take guess from users
                        for (int i = 0; i <= Users.size() - 1; i++) { // Check inputs from every user
                            if (i != WordSelectionUser) { // Check if the input is from anyone but the person who set
                                                          // the prompt
                                if (!UserInput.get(Users.get(i)).equals(UserInputCompare.get(Users.get(i)))) { // Check
                                                                                                               // if the
                                                                                                               // input
                                                                                                               // changed
                                                                                                               // since
                                                                                                               // last
                                                                                                               // time
                                    UserInputCompare.put(Users.get(i), UserInput.get(Users.get(i)));
                                    if (UserInputCompare.get(Users.get(i)).matches("[ABCDEFGHIJKLMNOPQRSTUVWXYZ]")) {
                                        System.out.println(Guesses);
                                        System.out.println(UserInput.get(Users.get(i)).charAt(0));
                                        System.out.println(Guesses.contains(UserInput.get(Users.get(i))));
                                        if (Guesses.contains(UserInput.get(Users.get(i)).charAt(0))) {

                                            clientMap.get(Users.get(i)).println("Letter: "
                                                    + UserInputCompare.get(Users.get(i)) + " already guessed");
                                        } else {
                                            SendAll(Users.get(i) + " guessed: " + UserInputCompare.get(Users.get(i)));
                                            Guess = UserInput.get(Users.get(i)).charAt(0);
                                            Guesses.add(Guess);
                                            State = 3;
                                        }

                                    } else {
                                        SendAll(Users.get(i) + ": " + UserInputCompare.get(Users.get(i)));
                                    }
                                }
                            }
                        }
                        break;
                    case 3: // Validate guesses, exit game if lost/won
                        if (Solution.indexOf(Guess) != -1) {
                            CorrectGuesses.add(Guess);
                            DisplaySolution = "";
                            for (int i = 0; i <= Solution.length() - 1; i++) {
                                if (Solution.charAt(i) == ' ') {
                                    DisplaySolution = DisplaySolution + " ";
                                } else {
                                    if (CorrectGuesses.contains(Solution.charAt(i))) {
                                        DisplaySolution = DisplaySolution + Solution.charAt(i);
                                    } else {
                                        DisplaySolution = DisplaySolution + "_";
                                    }
                                }
                                DisplaySolution = DisplaySolution + " ";
                            }
                        } else {
                            attempts = attempts - 1;
                        }
                        SendAll("Solution: " + DisplaySolution + " | Failed attempts left: " + attempts);
                        if (attempts == 0) {
                            SendAll("GAME LOST");
                            GameRunning = false;
                        } else if (!DisplaySolution.contains("_")) {
                            SendAll("GAME WON");
                            GameRunning = false;
                        } else {
                            State = 2;
                        }
                        break;

                    default:
                }

            }
            SequentialInput = false;
            LobbyLocked = false;
            return;
        }
    }

    static class Pong implements Runnable {

        private static long LastTime = System.currentTimeMillis();
        private static int BallvelX = 1, BallvelY = 1, BallposX = 5, BallposY = 5;
        private static int P1PaddleY = 8, P2PaddleY = 8;

        public void run() {
            SequentialInput = true;
            // try {
            while (true) {
                if (System.currentTimeMillis() - LastTime > 75) {
                    LastTime = System.currentTimeMillis();
                    if (!UserInput.get(Users.get(0)).isEmpty()) {
                        if (UserInput.get(Users.get(0)).toUpperCase().startsWith("W") && P1PaddleY != 0) {
                            P1PaddleY = P1PaddleY - 1;
                        }
                        if (UserInput.get(Users.get(0)).toUpperCase().startsWith("S") && P1PaddleY != 15) {
                            P1PaddleY = P1PaddleY + 1;
                        }
                    }

                    if (!UserInput.get(Users.get(1)).isEmpty()) {
                        if (UserInput.get(Users.get(1)).toUpperCase().startsWith("W") && P2PaddleY != 0) {
                            P2PaddleY = P2PaddleY - 1;
                        }
                        if (UserInput.get(Users.get(1)).toUpperCase().startsWith("S") && P2PaddleY != 15) {
                            P2PaddleY = P2PaddleY + 1;
                        }
                    }

                    String[] Display = new String[20];
                    for (int i = 0; i <= Display.length - 1; i++) {
                        Display[i] = "|                                                  |                                                  |";
                    }

                    for (int i = P1PaddleY; i <= P1PaddleY + 4; i++) {

                        Display[i] = Display[i].substring(0, 2) + ']' + Display[i].substring(3);
                    }

                    for (int i = P2PaddleY; i <= P2PaddleY + 4; i++) {
                        Display[i] = Display[i].substring(0, 100) + '[' + Display[i].substring(101);
                    }
                    Display[BallposY] = Display[BallposY].substring(0, BallposX) + '@'
                            + Display[BallposY].substring(BallposX + 1);

                    if (BallposY == 0) {
                        BallvelY = 1;
                    }
                    if (BallposY == 19) {
                        BallvelY = -1;
                    }

                    if (BallposX == 0) {
                        BallvelX = 1;
                    }
                    if (BallposX == 102) {
                        BallvelX = -1;
                    }
                    BallposY = BallposY + BallvelY;
                    BallposX = BallposX + BallvelX;

                    String Out = "";

                    for (String i : Display) {
                        Out = Out + i + "\n";
                    }
                    SendAll(Out);
                }
                // }
                // } catch (IOException e) {
                // e.printStackTrace();

            }
        }
    }

    static class Apple implements Runnable {

        private static long LastTime = System.currentTimeMillis();
        private static int Counter = 0;
        private static String Raw;
        private static String[] Frames;

        public void run() {
            try {
                Raw = new String(Files.readAllBytes((Paths.get("play.txt"))));
                Frames = Raw.split("SPLIT");
                SequentialInput = true;
                SendAll(Frames[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (true) {
                if (System.currentTimeMillis() - LastTime > 50) {
                    LastTime = System.currentTimeMillis();
                    SendAll(Frames[Counter]);
                    Counter += 1;
                    if (Counter >= Frames.length - 1) {
                        SequentialInput = false;
                        break;
                    }
                }
            }
        }
    }
}
