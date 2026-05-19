module hu.szatomi.mozi {
    requires javafx.controls;
    requires javafx.fxml;


    opens hu.szatomi.mozi to javafx.fxml;
    exports hu.szatomi.mozi;
}