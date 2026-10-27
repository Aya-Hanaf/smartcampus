package ma.ensak.smartcampus.model;

/** Type de salle. */
public enum RoomType {
    TD("Salle de TD"),
    WORK("Salle de travail"),
    LABORATORY("Laboratoire"),
    AMPHITHEATER("Amphithéâtre");

    private final String label;

    RoomType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
