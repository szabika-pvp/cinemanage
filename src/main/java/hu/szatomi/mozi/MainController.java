package hu.szatomi.mozi;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainController {

    public ComboBox<String> hallChoice;
    public CheckBox checkBox2D;
    public CheckBox checkBox3D;
    public CheckBox checkBoxIMAX;
    public ListView<Screening> resultList;
    public Label maximumLabel;
    public Label minimumLabel;
    public Label incomeLabel;

    private final ArrayList<Hall> halls = new ArrayList<>();
    private final ArrayList<Screening> screenings = new ArrayList<>();

    @FXML
    private void initialize() {
        
        loadFile("termek");
        loadFile("vetitesek");

        hallChoice.getItems().add("Összes");
        hallChoice.getItems().addAll(halls.stream().map(Hall::name).toList());

        hallChoice.valueProperty().addListener((_, _, h) -> {
            resultList.getItems().clear();

            resultList.getItems().addAll(
                    !h.equals("Összes")
                    ? screenings.stream()
                            .filter(s -> s.hallName().equals(h))
                            .toList()
                    : screenings.stream().sorted(Comparator.comparing(Screening::hallName)).toList()
            );
        });
    }

    private void loadFile(String fileName) {

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName + ".txt"))) {

            reader.readLine();
            reader.lines().forEach(line -> {
                if (fileName.equals("termek")) halls.add(parseHall(line));
                else screenings.add(parseScreening(line));
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private Hall parseHall(String line) {

        String[] data = line.split(";");

        String name = data[0];
        int seats = Integer.parseInt(data[1]);
        String canvasType = data[2];

        return new Hall(name, seats, canvasType);
    }

    private Screening parseScreening(String line) {

        String[] data = line.split(";");

        int ID = Integer.parseInt(data[0]);
        String hallName = data[1];
        String title = data[2];
        int ticketsSold = Integer.parseInt(data[3]);
        int ticketPrice = Integer.parseInt(data[4]);

        return new Screening(ID, hallName, title, ticketsSold, ticketPrice);
    }
}