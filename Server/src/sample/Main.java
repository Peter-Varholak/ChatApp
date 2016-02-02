package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        Parent root = loader.load();
        Controller con = loader.getController();
        primaryStage.setTitle("ChatApp - Server");

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();

        scene.getWindow().setOnCloseRequest(event -> {
            if (con.serverThread != null) {
                con.quitServer();
            }
        });
    }

    public static void main(String[] args) { launch(args); }
}
