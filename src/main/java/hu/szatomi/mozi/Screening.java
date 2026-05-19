package hu.szatomi.mozi;

public record Screening(int ID, String hallName, String title, int ticketsSold, int ticketPrice) {

    @Override
    public String toString() {
        return "Terem: %s | %s - %d".formatted(hallName, title, ticketPrice);
    }
}

