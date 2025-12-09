package models;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Представляет сотрудника организации.
 * <p>
 * Содержит персональные данные: ФИО, пол, дату рождения, подразделение, зарплату.
 * Все поля валидируются с помощью Jakarta Validation.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    /** Уникальный идентификатор сотрудника. Должен быть положительным */
    @NotNull(message = "ID не может быть null")
    @Positive(message = "ID должен быть положительным числом")
    private Long id;

    /** ФИО сотрудника. От 2 до 50 символов, не может быть пустым */
    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 50, message = "Имя должно содержать от 2 до 50 символов")
    private String name;

    /** Пол сотрудника. Не может быть null */
    @NotNull(message = "Пол не может быть null")
    private Gender gender;

    /** Подразделение, в котором работает сотрудник. Не может быть null */
    @NotNull(message = "Подразделение не может быть null")
    private Department department;

    /** Зарплата сотрудника. Должна быть больше 0 и не превышать 1 000 000 */
    @NotNull(message = "Зарплата не может быть null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Зарплата должна быть больше 0")
    @DecimalMax(value = "1000000.0", message = "Зарплата не может превышать 1,000,000")
    private BigDecimal salary;

    /** Дата рождения. Должна быть в прошлом и не может быть null */
    @NotNull(message = "Дата рождения не может быть null")
    @Past(message = "Дата рождения должна быть в прошлом")
    private LocalDate birthDate;

    /** Поддерживаемые форматы даты для парсинга */
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    /**
     * Парсит строку с датой в объект {@link LocalDate}.
     * Поддерживает несколько популярных форматов.
     *
     * @param dateString строка с датой
     * @return объект LocalDate
     * @throws IllegalArgumentException если строка пустая или имеет неверный формат
     */
    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new IllegalArgumentException("Дата не может быть пустой");
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateString.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Пробуем следующий формат
            }
        }
        throw new IllegalArgumentException("Неверный формат даты: " + dateString);
    }
}