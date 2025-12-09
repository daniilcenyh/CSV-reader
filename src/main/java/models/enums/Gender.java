package models.enums;

/**
 * Перечисление, представляющее пол сотрудника.
 * <p>
 * Поддерживает как английские, так и русские названия.
 * </p>
 */
public enum Gender {
    /** Мужской пол */
    MALE("Мужской"),
    /** Женский пол */
    FEMALE("Женский");

    /** Русскоязычное название пола */
    private final String russianName;

    /**
     * Конструктор перечисления.
     *
     * @param russianName название на русском языке
     */
    Gender(String russianName) {
        this.russianName = russianName;
    }

    /**
     * Возвращает русское название пола.
     *
     * @return строка с названием на русском
     */
    public String getRussianName() {
        return russianName;
    }

    /**
     * Преобразует строку в соответствующее значение перечисления.
     * Поддерживает как английские названия (MALE/FEMALE), так и русские.
     *
     * @param text входная строка
     * @return соответствующий объект Gender
     * @throws IllegalArgumentException если строка не соответствует ни одному значению
     */
    public static Gender fromString(String text) {
        if (text != null) {
            String trimmed = text.trim();
            for (Gender gender : Gender.values()) {
                if (trimmed.equalsIgnoreCase(gender.name()) ||
                        trimmed.equalsIgnoreCase(gender.russianName)) {
                    return gender;
                }
            }
        }
        throw new IllegalArgumentException("Неизвестный пол: " + text);
    }
}