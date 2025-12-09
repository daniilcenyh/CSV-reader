package models.enums;

public enum Gender {
    MALE("Мужской"),
    FEMALE("Женский");

    private final String russianName;

    Gender(String russianName) {
        this.russianName = russianName;
    }

    public String getRussianName() {
        return russianName;
    }

    public static Gender fromString(String text) {
        if (text != null) {
            for (Gender gender : Gender.values()) {
                if (text.trim().equalsIgnoreCase(gender.russianName) ||
                        text.trim().equalsIgnoreCase(gender.name())) {
                    return gender;
                }
            }
        }
        throw new IllegalArgumentException("Неизвестный пол: " + text);
    }
}