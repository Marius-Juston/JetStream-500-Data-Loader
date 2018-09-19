package dataLoader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("mainSelector.fxml"));
    primaryStage.setTitle("JetStream 500 Data Loader");
    primaryStage.setScene(new Scene(root));
    primaryStage.show();
  }
}
