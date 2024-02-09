package jfx;

import java.io.IOException;
import javafx.fxml.FXML;

public class ClientInsertionController {

    @FXML
    private void switchToClientInsertion() throws IOException {
        App.setRoot("insert_client");
    }
}