module jfx {
    requires javafx.controls;
    requires javafx.fxml;

    opens jfx to javafx.fxml;
    exports jfx;
}
