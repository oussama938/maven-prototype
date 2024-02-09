package jfx;

import java.io.IOException;
import javafx.fxml.FXML;

public class HomeController {

    @FXML
    private void switchToHome() throws IOException {
        App.setRoot("home");
    }
}
