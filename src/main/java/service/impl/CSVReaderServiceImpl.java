package service.impl;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import models.Department;
import models.Person;
import models.enums.Gender;
import service.CSVReaderService;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CSVReaderServiceImpl implements CSVReaderService {

    private static final char CSV_SEPARATOR = ';';
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final Validator validator;
    private final Map<String, Department> departmentCache = new HashMap<>();

    public CSVReaderServiceImpl() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    @Override
    public List<Person> readPeopleFromCSV(String csvFilePath) throws IOException {
        List<Person> people = new ArrayList<>();

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(csvFilePath)) {
            if (in == null) {
                throw new FileNotFoundException("Файл не найден в ресурсах: " + csvFilePath +
                        "\nУбедитесь, что файл находится в src/main/resources/");
            }

            // Диагностика первой строки
            System.out.println("=== НАЧАЛО ОБРАБОТКИ CSV ===");

            try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(in, "UTF-8"))
                    .withCSVParser(new CSVParserBuilder()
                            .withSeparator(CSV_SEPARATOR)
                            .withQuoteChar('"')
                            .build())
                    .withSkipLines(0) // Пропустим заголовок вручную
                    .build()) {

                String[] header = reader.readNext();
                if (header == null) {
                    throw new IOException("Файл пустой");
                }

                System.out.println("Заголовок файла: " + Arrays.toString(header));
                System.out.println("Ожидаемый формат: [id, name, gender, BirtDate, Division, Salary]");

                String[] nextLine;
                int lineNumber = 1; // Уже прочитали заголовок
                int successCount = 0;
                int errorCount = 0;

                while ((nextLine = reader.readNext()) != null) {
                    lineNumber++;

                    // Пропускаем пустые строки
                    if (nextLine.length == 0 ||
                            (nextLine.length == 1 && nextLine[0].trim().isEmpty())) {
                        continue;
                    }

                    // Выводим информацию о первых 3 строках данных
                    if (lineNumber <= 5) {
                        System.out.println("\nСтрока " + (lineNumber-1) + " (индекс в файле: " + lineNumber + "):");
                        System.out.println("  Raw data: " + Arrays.toString(nextLine));
                        System.out.println("  Field count: " + nextLine.length);
                    }

                    try {
                        Person person = parsePerson(nextLine, lineNumber);
                        validatePerson(person);
                        people.add(person);
                        successCount++;

                        // Выводим информацию о первых 3 успешно обработанных записях
                        if (successCount <= 3) {
                            System.out.printf("✓ Успешно обработан: %s (ID: %d, Отдел: %s, Дата: %s, ЗП: %.0f)%n",
                                    person.getName(), person.getId(),
                                    person.getDepartment().getName(),
                                    person.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                                    person.getSalary());
                        }

                    } catch (IllegalArgumentException e) {
                        errorCount++;
                        if (errorCount <= 5) {
                            System.err.printf("✗ Ошибка в строке %d: %s%n", lineNumber, e.getMessage());
                            System.err.println("  Данные: " + Arrays.toString(nextLine));
                        }
                    }
                }

                System.out.println("\n=== СТАТИСТИКА ОБРАБОТКИ ===");
                System.out.println("Всего строк данных: " + (lineNumber - 1));
                System.out.println("Успешно обработано: " + successCount);
                System.out.println("Ошибок обработки: " + errorCount);
                System.out.println("Уникальных подразделений: " + departmentCache.size());

                // Выводим информацию о подразделениях
                System.out.println("\nСписок подразделений:");
                List<Department> sortedDepartments = new ArrayList<>(departmentCache.values());
                sortedDepartments.sort(Comparator.comparing(Department::getName));

                for (Department dept : sortedDepartments) {
                    long employeeCount = people.stream()
                            .filter(p -> p.getDepartment().equals(dept))
                            .count();
                    System.out.printf("  %s (ID: %d) - сотрудников: %d%n",
                            dept.getName(), dept.getId(), employeeCount);
                }

            } catch (CsvValidationException e) {
                throw new IOException("Ошибка валидации CSV", e);
            }

        } catch (Exception e) {
            throw new IOException("Ошибка при чтении файла: " + e.getMessage(), e);
        }

        return Collections.unmodifiableList(people);
    }

    @Override
    public Map<String, Department> getDepartmentCache() {
        return Collections.unmodifiableMap(departmentCache);
    }

    private Person parsePerson(String[] csvLine, int lineNumber) {
        // Проверяем количество полей
        if (csvLine.length < 6) {
            throw new IllegalArgumentException(
                    String.format("Строка %d: Недостаточно данных. Ожидается 6 полей, получено: %d. " +
                                    "Проверьте правильность разделителя ';'",
                            lineNumber, csvLine.length));
        }

        try {
            // 1. ID (поле 0)
            Long id = parseId(csvLine[0].trim(), lineNumber);

            // 2. Имя (поле 1)
            String name = csvLine[1].trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Имя не может быть пустым");
            }

            // 3. Пол (поле 2) - Male/Female
            Gender gender = parseGender(csvLine[2].trim(), lineNumber);

            // 4. Дата рождения (поле 3) - формат dd.MM.yyyy
            LocalDate birthDate = parseBirthDate(csvLine[3].trim(), lineNumber);

            // 5. Подразделение (поле 4) - буква (A, B, C, ...)
            String divisionCode = csvLine[4].trim();
            if (divisionCode.isEmpty()) {
                divisionCode = "UNKNOWN";
            }

            // Создаем читаемое название подразделения
            String departmentName = "Отдел " + divisionCode;
            Department department = departmentCache.computeIfAbsent(
                    divisionCode, // Используем код как ключ
                    code -> new Department("Отдел " + code)
            );

            // 6. Зарплата (поле 5)
            BigDecimal salary = parseSalary(csvLine[5].trim(), lineNumber);

            Person person = new Person(id, name, gender, department, salary, birthDate);

            // Дополнительная проверка даты (должна быть в прошлом)
            if (birthDate.isAfter(LocalDate.now())) {
                System.err.printf("  Внимание: строка %d - дата рождения в будущем: %s%n",
                        lineNumber, birthDate);
            }

            // Дополнительная проверка зарплаты
            if (salary.compareTo(BigDecimal.ZERO) <= 0) {
                System.err.printf("  Внимание: строка %d - некорректная зарплата: %.2f%n",
                        lineNumber, salary);
            }

            return person;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("Строка %d: Ошибка преобразования числа", lineNumber), e);
        } catch (IllegalArgumentException e) {
            // Перебрасываем с указанием номера строки
            throw new IllegalArgumentException(
                    String.format("Строка %d: %s", lineNumber, e.getMessage()), e);
        }
    }

    private Long parseId(String idStr, int lineNumber) {
        if (idStr.isEmpty()) {
            // Генерируем ID на основе номера строки
            return (long) lineNumber * 1000L;
        }

        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный ID: " + idStr);
        }
    }

    private Gender parseGender(String genderStr, int lineNumber) {
        if (genderStr == null || genderStr.trim().isEmpty()) {
            System.err.printf("  Внимание: строка %d - пол не указан, используется MALE%n", lineNumber);
            return Gender.MALE;
        }

        String upperGender = genderStr.trim().toUpperCase();

        switch (upperGender) {
            case "MALE":
            case "M":
            case "МУЖ":
            case "МУЖСКОЙ":
                return Gender.MALE;

            case "FEMALE":
            case "F":
            case "ЖЕН":
            case "ЖЕНСКИЙ":
                return Gender.FEMALE;

            default:
                System.err.printf("  Внимание: строка %d - неизвестный пол '%s', используется MALE%n",
                        lineNumber, genderStr);
                return Gender.MALE;
        }
    }

    private LocalDate parseBirthDate(String dateStr, int lineNumber) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Дата рождения не может быть пустой");
        }

        String trimmedDate = dateStr.trim();

        try {
            return LocalDate.parse(trimmedDate, DATE_FORMATTER);
        } catch (Exception e) {
            // Пробуем другие форматы
            try {
                // Пробуем формат с дефисами
                if (trimmedDate.contains("-")) {
                    return LocalDate.parse(trimmedDate, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                }
                // Пробуем формат со слешами
                if (trimmedDate.contains("/")) {
                    return LocalDate.parse(trimmedDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }
            } catch (Exception e2) {
                // Не получилось
            }

            throw new IllegalArgumentException("Некорректный формат даты: " + trimmedDate +
                    " (ожидается дд.мм.гггг)");
        }
    }

    private BigDecimal parseSalary(String salaryStr, int lineNumber) {
        if (salaryStr == null || salaryStr.trim().isEmpty()) {
            System.err.printf("  Внимание: строка %d - зарплата не указана, используется 0%n", lineNumber);
            return BigDecimal.ZERO;
        }

        try {
            // Заменяем запятую на точку для корректного парсинга
            String normalized = salaryStr.trim().replace(",", ".");
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            System.err.printf("  Внимание: строка %d - некорректная зарплата '%s', используется 0%n",
                    lineNumber, salaryStr);
            return BigDecimal.ZERO;
        }
    }

    private void validatePerson(Person person) {
        Set<ConstraintViolation<Person>> violations = validator.validate(person);

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Ошибки валидации: ");

            for (ConstraintViolation<Person> violation : violations) {
                sb.append(violation.getPropertyPath())
                        .append(" - ")
                        .append(violation.getMessage())
                        .append("; ");
            }

            throw new IllegalArgumentException(sb.toString());
        }
    }
}