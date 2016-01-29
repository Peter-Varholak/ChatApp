package sample;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;


public class Controller implements Initializable {

    @FXML //fx:id="sendButton"
    private Button sendButton; //Value injected by FXMLLoader


    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        sendButton.setOnAction(event -> System.out.println("TEST"));
    }

}