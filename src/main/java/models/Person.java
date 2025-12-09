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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    @NotNull(message = "ID не может быть null")
    @Positive(message = "ID должен быть положительным числом")
    private Long id;

    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 50, message = "Имя должно содержать от 2 до 50 символов")
    private String name;

    @NotNull(message = "Пол не может быть null")
    private Gender gender;

    @NotNull(message = "Подразделение не может быть null")
    private Department department;

    @NotNull(message = "Зарплата не может быть null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Зарплата должна быть больше 0")
    @DecimalMax(value = "1000000.0", message = "Зарплата не может превышать 1,000,000")
    private BigDecimal salary;

    @NotNull(message = "Дата рождения не может быть null")
    @Past(message = "Дата рождения должна быть в прошлом")
    private LocalDate birthDate;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    };

    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new IllegalArgumentException("Дата не может быть пустой");
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateString.trim(), formatter);
            } catch (DateTimeParseException e) {
                // Пробуем следующий формат
            }
        }
        throw new IllegalArgumentException("Неверный формат даты: " + dateString);
    }
}