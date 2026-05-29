package hu.szatomi.mozi;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

        checkBox2D.selectedProperty().addListener(_ -> updateHallChoice());
        checkBox3D.selectedProperty().addListener(_ -> updateHallChoice());
        checkBoxIMAX.selectedProperty().addListener(_ -> updateHallChoice());

        reportButton.setOnAction(_ -> export());
    }

    private void loadFile(String fileName) {

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName + ".txt"))) {

            List<String> lines = reader.lines().toList();

            if (lines.size() <= 2) throw new RuntimeException("Nincs elég adat");

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);

                if (fileName.equals("termek")) {
                    Hall parsedHall = parseHall(line);
                    if (parsedHall == null) return;
                    halls.add(parseHall(line));
                }
                else if (fileName.equals("vetitesek")) {
                    Screening parsedScreening = parseScreening(line);
                    if (parsedScreening == null) return;
                    screenings.add(parseScreening(line));
                }
            }
            
        } catch (Exception e) {
            showError(e, "Hiba fájlok olvasása közben");
            System.exit(1);
        }
    }

    private Hall parseHall(String line) {

        try {
            String[] data = line.split(";");

            String name = data[0];
            int seats = Integer.parseInt(data[1]);
            String canvasType = data[2];

            return new Hall(name, seats, canvasType);

        } catch (Exception e) {
            showError(e, "Hiba a termek.txt fájl elemzése közben");
            System.exit(1);
            return null;
        }
    }

    private Screening parseScreening(String line) {

        try {
            String[] data = line.split(";");

            int ID = Integer.parseInt(data[0]);
            String hallName = data[1];
            String title = data[2];
            int ticketsSold = Integer.parseInt(data[3]);
            int ticketPrice = Integer.parseInt(data[4]);

            return new Screening(ID, hallName, title, ticketsSold, ticketPrice);

        } catch (Exception e) {
            showError(e, "Hiba a vetitesek.txt fájl elemzése közben");
            System.exit(1);
            return null;
        }
    }

    private void updateHallChoice() {
        boolean is2D = checkBox2D.isSelected();
        boolean is3D = checkBox3D.isSelected();
        boolean isIMAX = checkBoxIMAX.isSelected();
        boolean allOrNone = (is2D && is3D && isIMAX) || (!is2D && !is3D && !isIMAX);

        List<String> filteredHalls = halls.stream()
                .filter(h -> allOrNone ||
                        (is2D && h.canvasType().equals("2D")) ||
                        (is3D && h.canvasType().equals("3D")) ||
                        (isIMAX && h.canvasType().equals("IMAX")))
                .map(Hall::name)
                .toList();

        String previousSelection = hallChoice.getValue();

        hallChoice.getItems().clear();
        hallChoice.getItems().add("Összes");
        hallChoice.getItems().addAll(filteredHalls);

        if (previousSelection != null && hallChoice.getItems().contains(previousSelection)) {
            hallChoice.getSelectionModel().select(previousSelection);
        } else {
            hallChoice.getSelectionModel().select("Összes");
        }

        search(hallChoice.getValue());
    }

    private void search(String hallName) {

        if (hallName == null) return;

        List<Screening> searchResults;

        try {
            if (!hallName.equals("Összes")) {
                searchResults = screenings.stream()
                        .filter(s -> s.hallName().equals(hallName))
                        .toList();
            } else {
                boolean is2D = checkBox2D.isSelected();
                boolean is3D = checkBox3D.isSelected();
                boolean isIMAX = checkBoxIMAX.isSelected();
                boolean allOrNone = (is2D && is3D && isIMAX) || (!is2D && !is3D && !isIMAX);

                searchResults = screenings.stream()
                        .filter(s -> {
                            Hall hall = getHall(s);
                            if (hall == null || allOrNone) return true;
                            return (is2D && hall.canvasType().equals("2D")) ||
                                   (is3D && hall.canvasType().equals("3D")) ||
                                   (isIMAX && hall.canvasType().equals("IMAX"));
                        })
                        .sorted(Comparator.comparing(Screening::hallName))
                        .toList();
            }
        } catch (NullPointerException e) {
            searchResults = new ArrayList<>();
        }

        resultList.getItems().clear();
        resultList.getItems().addAll(searchResults);

        selectedHall = hallName;
        maximumLabel.setText(maximum() == null
                ? "-"
                : "%s - %d jegy eladva".formatted(
                        Objects.requireNonNull(maximum()).title(),
                        Objects.requireNonNull(maximum()).ticketsSold()
                )
        );
        minimumLabel.setText(minimum() == null || Objects.requireNonNull(minimum()).ticketsSold() >= 10 ?
                "-" :
                "%s - %d jegy eladva".formatted(
                        Objects.requireNonNull(minimum()).title(),
                        Objects.requireNonNull(minimum()).ticketsSold()
                )
        );

        if (!Objects.equals(selectedHall, "Összes"))
            incomeLabel.setText("Bevétel: %d Ft".formatted(calculateIncome(selectedHall)));
        else
            incomeLabel.setText("");

        reportButton.setDisable(resultList.getItems().isEmpty());
    }

    private Hall getHall(Screening screening) {
        return halls.stream().filter(h -> h.name().equals(screening.hallName())).findFirst().orElse(null);
    }

    private Screening maximum() {
        List<Screening> sortedScreenings;

        sortedScreenings = screenings.stream()
                .filter(s -> selectedHall.equals("Összes") || s.hallName().equals(selectedHall))
                .sorted((o1, o2) -> o2.ticketsSold() - o1.ticketsSold())
                .toList();

        return !sortedScreenings.isEmpty() ? sortedScreenings.getFirst() : null;
    }

    private Screening minimum() {
        List<Screening> sortedScreenings;

        sortedScreenings = screenings.stream()
                .filter(s -> (selectedHall.equals("Összes") || s.hallName().equals(selectedHall)))
                .sorted(Comparator.comparingInt(Screening::ticketsSold))
                .toList();

        return !sortedScreenings.isEmpty() ? sortedScreenings.getFirst() : null;
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
            showError(e, "Hiba az exportálás közben");
        }
    }

    private void showError(Exception e, String title) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}