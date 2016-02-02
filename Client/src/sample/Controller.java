package sample;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class Controller implements Initializable {

    @FXML private VBox middle;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    @FXML private HBox loginArea;
    @FXML private MenuItem menu1;
    @FXML private MenuItem menu2;
    @FXML private Label status;
    @FXML private Button sendButton;
    @FXML private TextField serverAddress;
    @FXML private TextField username;
    @FXML private Button connectButton;


    Thread clientThread;
    Client client;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        chatArea.setEditable(false);
        chatInput.setEditable(false);
        sendButton.setDisable(true);
        menu1.setOnAction(event -> login());
        menu2.setOnAction(event1 -> quitSession());
        sendButton.setOnAction(event -> sendMessage(chatInput.getText()));
        chatInput.setOnAction(event -> sendMessage(chatInput.getText()));
        serverAddress.setOnAction(event -> login());
        username.setOnAction(event -> login());
        connectButton.setOnAction(event -> login());
    }

    private void login() {
        String serverIP = serverAddress.getText().trim();
        String nick = username.getText().trim();
        if (serverIP.length() == 0 || serverIP.equals("") || nick.length() == 0 || nick.equals("")) {
            status.setText("All fields are required");
        } else {
            try {
                client = new Client(serverIP, nick, chatArea, loginArea, middle, sendButton, chatInput, status, menu1, menu2);
                clientThread = new Thread(client);
                clientThread.setDaemon(true);
                clientThread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void quitSession() {
        if (client.getConnected()) {
            client.sendMessage("#disconnect!");
        }
        clientThread.stop();
        clientThread = null;
        client.changeGUI(false);
    }

    private void sendMessage(String message) {
        client.sendMessage(message);
        chatInput.setText("");
    }
}