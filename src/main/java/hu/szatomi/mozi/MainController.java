package hu.szatomi.mozi;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    public Button reportButton;


    private final ArrayList<Hall> halls = new ArrayList<>();
    private final ArrayList<Screening> screenings = new ArrayList<>();

    private String selectedHall = "Összes";

    @FXML
    private void initialize() {

        loadFile("termek");
        loadFile("vetitesek");

        hallChoice.getItems().add("Összes");
        hallChoice.getSelectionModel().select("Összes");
        hallChoice.getItems().addAll(halls.stream().map(Hall::name).toList());

        search("Összes");

        hallChoice.valueProperty().addListener((_, _, h) -> search(h));

        checkBox2D.selectedProperty().addListener(_ -> search(selectedHall));
        checkBox3D.selectedProperty().addListener(_ -> search(selectedHall));
        checkBoxIMAX.selectedProperty().addListener(_ -> search(selectedHall));

        reportButton.setOnAction(_ -> export());
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

    private void search(String hallName) {

        resultList.getItems().clear();
        resultList.getItems().addAll(
                !hallName.equals("Összes")
                        ? screenings.stream()
                                .filter(s -> s.hallName().equals(hallName))
                                .toList()
                        : screenings.stream()
                                .filter(s -> (checkBox2D.isSelected() && getHall(s).canvasType().equals("2D")) ||
                                        (checkBox3D.isSelected() && getHall(s).canvasType().equals("3D")) ||
                                        (checkBoxIMAX.isSelected() && getHall(s).canvasType().equals("IMAX")))
                                .sorted(Comparator.comparing(Screening::hallName)).toList()
        );

        selectedHall = hallName;
        maximumLabel.setText(maximum() == null ?
                "-" :
                "%s - %d jegy eladva".formatted(minimum().title(), minimum().ticketsSold()));
        minimumLabel.setText(minimum() == null || minimum().ticketsSold() >= 10 ?
                "-" :
                "%s - %d jegy eldava".formatted(maximum().title(), maximum().ticketsSold()));
        incomeLabel.setText("Bevétel: %d Ft".formatted(calculateIncome(selectedHall)));

        if (resultList.getItems().isEmpty()) reportButton.setDisable(true);
        else reportButton.setDisable(false);

        if (!hallName.equals("Összes")) {
            checkBox2D.setDisable(true);
            checkBox3D.setDisable(true);
            checkBoxIMAX.setDisable(true);
        } else {
            checkBox2D.setDisable(false);
            checkBox3D.setDisable(false);
            checkBoxIMAX.setDisable(false);
        }
    }

    private Hall getHall(Screening screening) {
        return halls.stream().filter(h -> h.name().equals(screening.hallName())).findFirst().orElse(null);
    }

    private Screening maximum() {

        return screenings.stream()
                .filter(s -> selectedHall.equals("Összes") || s.hallName().equals(selectedHall))
                .sorted((o1, o2) -> o2.ticketsSold() - o1.ticketsSold())
                .toList()
                .getFirst();
    }

    private Screening minimum() {

        return screenings.stream()
                .filter(s -> (selectedHall.equals("Összes") || s.hallName().equals(selectedHall)))
                .sorted(Comparator.comparingInt(Screening::ticketsSold))
                .toList()
                .getFirst();
    }

    private int calculateIncome(String hallName) {

        return screenings.stream()
                .filter(s -> s.hallName().equals(hallName))
                .map(s -> s.ticketPrice() * s.ticketsSold())
                .mapToInt(Integer::intValue).sum();
    }

    private void export() {

        try {

            LocalDateTime currentTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String formattedTime = currentTime.format(formatter);
            String simpleFormattedTime = formattedTime.replace(" ", "-").replace(":", "-");

            BufferedWriter writer = new BufferedWriter(new FileWriter("export_%s.txt".formatted(simpleFormattedTime)));

            writer.write("Exportálás időpontja: %s%n%n".formatted(formattedTime));

            for(Screening s : resultList.getItems()) {
                writer.write(s.toString() + "\n");
            }

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}