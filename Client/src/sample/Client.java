package sample;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Iceblaze on 29. 1. 2016.
 * For additional information contact me on peter.varholak@akademiasovy.sk
 */
public class Client implements Runnable {

    private TextArea chatArea;
    private HBox loginArea;
    private VBox middle;
    private Button sendButton;
    private TextField chatInput;
    private Label status;
    private MenuItem menu1;
    private MenuItem menu2;
    private Socket connection = null;
    private DataInputStream input = null;
    private DataOutputStream output = null;
    private String username = null;
    private String serverIP = null;
    private boolean connected;

    public Client(String ip, String nick, TextArea ta, HBox la, VBox mid, Button sb, TextField ci, Label s, MenuItem m1, MenuItem m2) {
        chatArea = ta;
        loginArea = la;
        middle = mid;
        sendButton = sb;
        chatInput = ci;
        status = s;
        menu1 = m1;
        menu2 = m2;
        serverIP = ip;
        nick = nick.toLowerCase();
        username = nick.substring(0, 1).toUpperCase() + nick.substring(1);
        connected = false;
    }

    public boolean getConnected() {
        return connected;
    }

    private void startChat() {
        try {
            changeGUI(true);
            try {
                while (connected) {
                    String reply = input.readUTF();
                    if (isHeartBeat(reply)) {
                        sendMessage("#");
                    } else {
                        showMessage(reply);
                    }
                }
            } catch (Exception e) {
                showMessage("* Server is offline.\n");
            } finally {
                cleanUp();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("* Connection error\n");
        }
    }

    public void testName() {
        try {
            output.writeUTF(username);
            String reply = input.readUTF();
            if (reply.contains("successful")) {
                showMessage(reply);
                startChat();
                changeGUI(false);
            } else {
                showMessage(reply);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("* Connection error\n");
        }
    }

    public void changeGUI(boolean online) {
        if (online && !connected) {
            Platform.runLater(() -> {
                status.setText("Connected as " + username);
                chatInput.setEditable(true);
                chatInput.setText("");
                sendButton.setDisable(false);
                menu1.setVisible(false);
                menu2.setVisible(true);
                middle.getChildren().remove(loginArea);
            });
            connected = true;
        } else if (!online && connected) {
            showMessage("* You have been disconnected\n");
            Platform.runLater(() -> {
                status.setText("Disconnected");
                chatInput.setEditable(false);
                sendButton.setDisable(true);
                menu1.setVisible(true);
                menu2.setVisible(false);
                middle.getChildren().add(loginArea);
            });
            connected = false;
        }
    }

    private void cleanUp() {
        try {
            input.close();
            output.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isHeartBeat(String message) {
        return message.startsWith("SERVER: #");
    }

    public void sendMessage(String message) {
        message = message.trim();
        if (!message.equals("")) {
            try {
                output.writeUTF(message);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showMessage(final String text) {
        Platform.runLater(() -> chatArea.appendText(text));
    }

    @Override
    public void run() {
        try {
            connection = new Socket(serverIP, 1379);
            input = new DataInputStream(connection.getInputStream());
            output = new DataOutputStream(connection.getOutputStream());
            testName();
        } catch (IOException e) {
            showMessage("* Server is offline\n");
            changeGUI(false);
        }
    }
}