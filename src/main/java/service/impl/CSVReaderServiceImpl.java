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
import java.util.*;

/**
 * Реализация сервиса для чтения и парсинга CSV-файлов со сведениями о сотрудниках.
 * <p>
 * Поддерживает:
 * <ul>
 *   <li>Чтение из classpath (src/main/resources)</li>
 *   <li>Разделитель — точка с запятой (;)</li>
 *   <li>Кэширование подразделений</li>
 *   <li>Валидацию объектов через Hibernate Validator</li>
 *   <li>Гибкий парсинг дат и пола</li>
 * </ul>
 * </p>
 */
public class CSVReaderServiceImpl implements CSVReaderService {

    private static final char CSV_SEPARATOR = ';';
    /** Валидатор для проверки объектов Person */
    private final Validator validator;

    /** Кэш подразделений: код подразделения → объект Department */
    private final Map<String, Department> departmentCache = new HashMap<>();

    /** Создаёт сервис и инициализирует валидатор */
    public CSVReaderServiceImpl() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    /**
     * Читает сотрудников из CSV-файла, расположенного в classpath.
     *
     * @param csvFilePath путь к файлу в ресурсах (например, "people.csv")
     * @return список успешно распарсенных и валидированных сотрудников
     * @throws IOException если файл не найден или произошла ошибка чтения
     */
    @Override
    public List<Person> readPeopleFromCSV(String csvFilePath) throws IOException {
        List<Person> people = new ArrayList<>();

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(csvFilePath)) {
            if (in == null) {
                throw new FileNotFoundException("Файл не найден в ресурсах: " + csvFilePath +
                        "\nПоместите файл в src/main/resources/");
            }

            // Для диагностики сначала прочитаем первые строки
            System.out.println("=== ДИАГНОСТИКА ФАЙЛА ===");

            // Создаем BufferedReader для диагностики
            BufferedReader diagnosticReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            // Читаем первые 3 строки для диагностики
            System.out.println("Первые 3 строки файла:");
            for (int i = 0; i < 3; i++) {
                String line = diagnosticReader.readLine();
                if (line != null) {
                    System.out.println((i+1) + ": " + line);
                    System.out.println("   Разделение по ';': " + Arrays.toString(line.split(";")));
                    System.out.println("   Длина строки: " + line.length());
                }
            }

            diagnosticReader.close();

            // Закрываем и переоткрываем поток
            in.close();

            // Снова открываем поток для CSVReader
            InputStream newIn = getClass().getClassLoader().getResourceAsStream(csvFilePath);
            if (newIn == null) {
                throw new FileNotFoundException("Не удалось переоткрыть файл: " + csvFilePath);
            }

            // Используем правильный конструктор CSVReader
            try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(newIn, "UTF-8"))
                    .withCSVParser(new CSVParserBuilder()
                            .withSeparator(CSV_SEPARATOR)
                            .withQuoteChar('"')
                            .withEscapeChar('\\')
                            .build())
                    .withSkipLines(1) // Пропускаем заголовок
                    .build()) {

                String[] nextLine;
                int lineNumber = 0;
                int processedCount = 0;

                System.out.println("\n=== НАЧАЛО ОБРАБОТКИ ===");

                while ((nextLine = reader.readNext()) != null) {
                    lineNumber++;

                    // Пропускаем пустые строки
                    if (nextLine.length == 0 ||
                            (nextLine.length == 1 && nextLine[0].trim().isEmpty())) {
                        continue;
                    }

                    // Выводим информацию о первых 5 строках
                    if (lineNumber <= 5) {
                        System.out.println("Строка " + lineNumber + ":");
                        System.out.println("  Raw: " + Arrays.toString(nextLine));
                        System.out.println("  Длина массива: " + nextLine.length);
                        for (int i = 0; i < nextLine.length; i++) {
                            System.out.println("  [" + i + "]: '" + nextLine[i] + "'");
                        }
                    }

                    try {
                        Person person = parsePerson(nextLine, lineNumber); // Вызываем с 2 параметрами
                        validatePerson(person);
                        people.add(person);
                        processedCount++;

                        // Выводим информацию о первых 3 успешных записях
                        if (processedCount <= 3) {
                            System.out.printf("✓ Успешно: %s (ID: %d, Отдел: %s)%n",
                                    person.getName(), person.getId(), person.getDepartment().getName());
                        }

                    } catch (IllegalArgumentException e) {
                        if (lineNumber <= 10) { // Ограничиваем вывод ошибок
                            System.err.println("✗ Ошибка в строке " + lineNumber + ": " + e.getMessage());
                            System.err.println("  Данные: " + Arrays.toString(nextLine));
                        }
                    }
                }

                System.out.println("\n=== РЕЗУЛЬТАТЫ ===");
                System.out.println("Всего строк прочитано: " + lineNumber);
                System.out.println("Успешно обработано: " + processedCount);
                System.out.println("Ошибок: " + (lineNumber - processedCount));

            } catch (CsvValidationException e) {
                throw new IOException("Ошибка валидации CSV", e);
            }

        } catch (Exception e) {
            throw new IOException("Ошибка при чтении файла: " + e.getMessage(), e);
        }

        return Collections.unmodifiableList(people);
    }

    /**
     * Возвращает кэш подразделений.
     *
     * @return неизменяемую копию кэша подразделений (код → объект)
     */
    @Override
    public Map<String, Department> getDepartmentCache() {
        return Collections.unmodifiableMap(departmentCache);
    }

    // Старый метод (оставляем для обратной совместимости, если где-то используется)
    private Person parsePerson(String[] csvLine) {
        return parsePerson(csvLine, -1); // Вызываем новый метод с номером строки -1
    }

    // Новый метод с номером строки для лучшего сообщения об ошибках
    private Person parsePerson(String[] csvLine, int lineNumber) {
        if (csvLine.length < 6) {
            String errorMsg = "Недостаточно данных в строке. Ожидается 6 полей, получено: " + csvLine.length;
            if (lineNumber > 0) {
                errorMsg = "Строка " + lineNumber + ": " + errorMsg;
            }
            throw new IllegalArgumentException(errorMsg);
        }

        try {
            // ID
            Long id;
            try {
                id = Long.parseLong(csvLine[0].trim());
            } catch (NumberFormatException e) {
                // Если ID не число, используем номер строки или генерируем
                if (lineNumber > 0) {
                    id = (long) lineNumber;
                } else {
                    id = System.currentTimeMillis() % 1000000; // Простой генератор
                }
                System.out.printf("  Строка %d: Используется сгенерированный ID: %d%n",
                        lineNumber > 0 ? lineNumber : 0, id);
            }

            // Имя
            String name = csvLine[1].trim();
            if (name.isEmpty()) {
                name = "Неизвестно_" + id;
                if (lineNumber > 0) {
                    System.out.printf("  Строка %d: Пустое имя, заменено на: %s%n", lineNumber, name);
                }
            }

            // Пол
            Gender gender;
            try {
                gender = Gender.fromString(csvLine[2].trim());
            } catch (IllegalArgumentException e) {
                gender = Gender.MALE; // Значение по умолчанию
                if (lineNumber > 0) {
                    System.out.printf("  Строка %d: Неизвестный пол '%s', используется MALE%n",
                            lineNumber, csvLine[2].trim());
                }
            }

            // Подразделение
            String departmentName = csvLine[3].trim();
            if (departmentName.isEmpty()) {
                departmentName = "Без_отдела";
            }
            Department department = departmentCache.computeIfAbsent(
                    departmentName,
                    Department::new
            );

            // Зарплата
            BigDecimal salary;
            String salaryStr = csvLine[4].trim().replace(",", ".");
            if (salaryStr.isEmpty()) {
                salary = BigDecimal.ZERO;
            } else {
                try {
                    salary = new BigDecimal(salaryStr);
                } catch (NumberFormatException e) {
                    salary = BigDecimal.ZERO;
                    if (lineNumber > 0) {
                        System.err.printf("  Строка %d: Некорректная зарплата '%s', используется 0%n",
                                lineNumber, salaryStr);
                    }
                }
            }

            // Дата рождения
            LocalDate birthDate;
            String dateStr = csvLine[5].trim();
            try {
                birthDate = Person.parseDate(dateStr);
            } catch (IllegalArgumentException e) {
                // Пробуем разные форматы дат
                birthDate = parseDateFlexible(dateStr, lineNumber);
            }

            return new Person(id, name, gender, department, salary, birthDate);

        } catch (NumberFormatException e) {
            String errorMsg = "Ошибка преобразования числа: " + e.getMessage();
            if (lineNumber > 0) {
                errorMsg = "Строка " + lineNumber + ": " + errorMsg;
            }
            throw new IllegalArgumentException(errorMsg, e);
        } catch (IllegalArgumentException e) {
            String errorMsg = "Ошибка в данных: " + e.getMessage();
            if (lineNumber > 0) {
                errorMsg = "Строка " + lineNumber + ": " + errorMsg;
            }
            throw new IllegalArgumentException(errorMsg, e);
        }
    }

    private LocalDate parseDateFlexible(String dateString, int lineNumber) {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new IllegalArgumentException("Дата не может быть пустой");
        }

        dateString = dateString.trim();

        // Пробуем разные форматы дат
        String[] patterns = {
                "dd.MM.yyyy", "dd-MM-yyyy", "dd/MM/yyyy",
                "yyyy.MM.dd", "yyyy-MM-dd", "yyyy/MM/dd",
                "dd.MM.yy", "dd-MM-yy", "dd/MM/yy",
                "MM/dd/yyyy", "MM-dd-yyyy", "MM/dd/yy", "MM-dd-yy"
        };

        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateString, java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {
                // Пробуем следующий формат
            }
        }

        // Если ничего не помогло, выбрасываем исключение
        throw new IllegalArgumentException("Не удалось распознать дату: " + dateString +
                " (строка " + lineNumber + ")");
    }

    private void validatePerson(Person person) {
        Set<ConstraintViolation<Person>> violations = validator.validate(person);

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Ошибки валидации для человека с ID=")
                    .append(person.getId())
                    .append(":\n");

            for (ConstraintViolation<Person> violation : violations) {
                sb.append("  - ")
                        .append(violation.getPropertyPath())
                        .append(": ")
                        .append(violation.getMessage())
                        .append("\n");
            }

            throw new IllegalArgumentException(sb.toString());
        }
    }
}