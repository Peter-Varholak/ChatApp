package sample;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.jetbrains.annotations.Nullable;

/**
 * Created by PC-5 on 29. 1. 2016.
 * For additional information contact me on peter.varholak@akademiasovy.sk
 */
public class Server implements Runnable {

    private TextArea chatArea;
    private ServerSocket server;
    private Socket connection;
    private Vector<Client> clients;
    private boolean serverOnline = false;

    public Server(TextArea ta) {
        chatArea = ta;
        serverOnline = true;
    }

    public void startServer() {
        showMessage("* Server running\n");
        try {
            server = new ServerSocket(1379);
            clients = new Vector<>();
            Timer timer = new Timer();
            timer.schedule(new HeartBeat(), 0, 20000);
            try {
                while (serverOnline) {
                    waitForConnection();
                }
                cleanUp();
            } catch (EOFException e) {
                showMessage("* Connection ended.\n");
            } finally {
                cleanUp();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForConnection() throws IOException {
        connection = server.accept();
        Verifier vf = new Verifier(connection);
        showMessage("* " + connection.getInetAddress().getHostName() + " connected.\n");
        vf.setDaemon(true);
        vf.start();
    }

    public void quitServer() {
        showMessage("* Server stopped\n");
        serverOnline = false;
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connection = null;
        server = null;
    }

    private void cleanUp() {
        try {
            for (Client c : clients) {
                c.input.close();
                c.output.close();
                c.socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //TODO: rewrite this into smaller methods
    public void resolveMessage(String sender, String msg) {
        if (msg.startsWith("!")) {
            if (msg.startsWith("!kick")) {
                StringTokenizer tokens = new StringTokenizer(msg, " ");
                tokens.nextToken(); //get rid of !kick
                String targetName = tokens.nextToken();
                String finalMessage = "";
                while (tokens.hasMoreTokens()) {
                    finalMessage += " " + tokens.nextToken();
                }
                Client target = findClientByUsername(targetName);
                if (target != null) {
                    disconnectClient(targetName);
                    if (finalMessage.equalsIgnoreCase("")) {
                        sendPublic("SERVER", targetName + " was kicked from the chat.\n");
                    } else {
                        sendPublic("SERVER", targetName + " was kicked from the chat. Reason: " + finalMessage + "\n");
                    }
                } else {
                    showMessage("* User \"" + targetName + "\" doesn't exist.\n");
                }

            }
        } else if (msg.startsWith("@")) {
            String sent = msg.substring(1);
            StringTokenizer tokens = new StringTokenizer(sent, " ");
            String targetName = tokens.nextToken();
            String finalMessage = "";
            while (tokens.hasMoreTokens()) {
                finalMessage += " " + tokens.nextToken();
            }
            Client targetClient = findClientByUsername(targetName);
            Client senderClient = findClientByUsername(sender);
            if (targetClient != null && senderClient != null) {
                if (targetClient.username.equalsIgnoreCase(senderClient.username)) {
                    sendMessage("SERVER", senderClient.output, "You can't send messages to yourself.");
                } else {
                    sendPrivate(sender, targetClient, finalMessage);
                    sendPrivate(sender, senderClient, finalMessage);
                }
            } else if (senderClient != null){
                showMessage("* User \"" + targetName + "\" doesn't exist.\n");
                sendMessage("SERVER", senderClient.output, "User \"" + targetName + "\" doesn't exist.\n");
            } else if (sender.equalsIgnoreCase("SERVER")) {
                if (targetClient != null) {
                    sendPrivate(sender, targetClient, finalMessage);
                } else {
                    showMessage("* User \"" + targetName + "\" doesn't exist.\n");
                }
            }
        } else if (msg.startsWith("#")) {
            if (msg.equalsIgnoreCase("#disconnect!")) {
                disconnectClient(sender);
                sendPublic("SERVER", sender + " left chat.\n");
            }
        } else {
            sendPublic(sender, msg);
        }
    }

    public void sendPublic(String sender, String message) {
        try {
            message = message.trim();
            if (!message.equals("")) {
                for (Client c : clients) {
                    sendMessage(sender, c.output, message);
                }
                showMessage(sender + ": " + message + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPrivate(String sender, Client target, String message) {
        try {
            message = message.trim();
            if (!message.equals("")) {
                sendMessage("!" + sender, target.output, message);
                if (!sender.equalsIgnoreCase(target.username)) {
                    showMessage(sender + " -> @" + target.username + ": " + message + "\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String sender, DataOutputStream output, String message) {
        try {
            message = message.trim();
            if (!message.equals("")) {
                output.writeUTF(sender + ": " + message + "\n");
                output.flush();
            }
        } catch (IOException e) {
            showMessage("* User doesn't exist..\n");
        }
    }

    private void showMessage(final String text) {
        Platform.runLater(() -> chatArea.appendText(text));
    }

    private void heartBeat() {
        for (Client c : clients) {
            sendMessage("SERVER", c.output, "#");
        }
    }

    private boolean clientExists(String username) {
        for (Client c : clients) {
            if (c.username.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    private void disconnectClient(String username) {
        for (int i = 0; i < clients.size(); i++) {
            Client client = clients.get(i);
            String un = client.username;
            if (un.equalsIgnoreCase(username)) {
                clients.remove(i);
                try {
                    client.socket.close();
                    client.input.close();
                    client.output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    @Nullable
    private Client findClientByUsername(String username) {
        if (clientExists(username)) {
            for (Client c : clients) {
                if (c.username.equalsIgnoreCase(username)) {
                    return c;
                }
            }
        }
        return null;
    }

    public class Verifier extends Thread {

        private Socket socket;
        private DataInputStream input;
        private DataOutputStream output;
        private String username;

        public Verifier(Socket sk) {
            try {
                socket = sk;
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Checks if UN already exists
        public boolean check() {
            try {
                username = input.readUTF();
                showMessage("* Username request: \"" + username + "\"\n");
                if (!username.matches("[a-zA-Z]{3,}")) {
                    sendMessage("SERVER", output, "Username can contain only letters and must be at least 3 characters long.\n");
                    showMessage("* " + socket.getInetAddress().getHostName() + " failed to log in due to invalid username.\n");
                } else if (clientExists(username)) {
                    sendMessage("SERVER", output, "Username \"" + username + "\" is already taken.\n");
                    showMessage("* " + socket.getInetAddress().getHostName() + " failed to log in due to duplicate username.\n");
                } else {
                    sendMessage("SERVER", output, "Login successful.\n");
                    showMessage("* " + username + " logged in successfully.\n");
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        //Check if UN already exists, adds to clients vector and starts the thread
        public void run() {
            if (check()) {
                username = username.toLowerCase();
                String un = username.substring(0, 1).toUpperCase() + username.substring(1);
                Client client = new Client(socket, un);
                clients.add(client);
                client.setDaemon(true);
                client.start();
            }
        }
    }

    public class Client extends Thread {

        private Socket socket;
        private DataInputStream input;
        private DataOutputStream output;
        private String username;

        public Client(Socket skt, String un) {
            socket = skt;
            username = un;
            try {
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            sendPublic("SERVER", username + " joined chat.\n");
            try {
                while (true) {
                    String listen = input.readUTF();
                    resolveMessage(username, listen);
                }
            } catch (Exception e) {
                //Client dropped
                showMessage("* " + username + " lost connection to the server.\n");
                disconnectClient(username);
                sendPublic("SERVER", username + " dropped from chat.\n");
            }
        }
    }

    class HeartBeat extends TimerTask {
        public void run() {
            heartBeat();
        }
    }

    @Override
    public void run() {
        try {
            startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}