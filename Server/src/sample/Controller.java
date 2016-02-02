package sample;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;


public class Controller implements Initializable {

    @FXML private MenuItem menu1;
    @FXML private MenuItem menu2;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    @FXML private Button sendButton;
    @FXML private Label status;

    Thread serverThread = null;
    Server server = null;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        chatArea.setEditable(false);
        chatInput.setEditable(false);
        sendButton.setDisable(true);
        menu1.setOnAction(event -> startServer());
        menu2.setOnAction(event -> quitServer());
        sendButton.setOnAction(event -> sendMessage(chatInput.getText()));
        chatInput.setOnAction(event -> sendMessage(chatInput.getText()));
    }

    private void startServer() {
        try {
            server = new Server(chatArea);
            serverThread = new Thread(server);
            serverThread.setDaemon(true);
            serverThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        status.setText("Server online");
        chatInput.setEditable(true);
        chatInput.setText("");
        sendButton.setDisable(false);
        menu1.setVisible(false);
        menu2.setVisible(true);
    }

    public void quitServer() {
        status.setText("Server offline");
        chatInput.setEditable(false);
        serverThread.stop();
        serverThread = null;
        server.quitServer();
        menu1.setVisible(true);
        menu2.setVisible(false);
    }

    private void sendMessage(String message) {
        server.resolveMessage("SERVER", message);
        chatInput.setText("");
    }
}