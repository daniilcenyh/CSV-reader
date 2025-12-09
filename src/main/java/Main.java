import models.Person;
import service.CSVReaderService;
import service.impl.CSVReaderServiceImpl;

import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== CSV READER - ОБРАБОТКА ФАЙЛА С СОТРУДНИКАМИ ===");

        // Имя файла - укажите ваше имя файла
        String csvFileName = "people.csv"; // или "employees.csv" или другое имя вашего файла

        CSVReaderService csvReaderService = new CSVReaderServiceImpl();

        try {
            // Чтение данных из CSV
            List<Person> people = csvReaderService.readPeopleFromCSV(csvFileName);

            // Вывод итоговой статистики
            System.out.println("\n" + "=".repeat(80));
            System.out.println("ИТОГОВАЯ СТАТИСТИКА");
            System.out.println("=".repeat(80));

            System.out.printf("Всего сотрудников загружено: %d%n", people.size());

            // Агрегированная статистика
            double avgSalary = people.stream()
                    .mapToDouble(p -> p.getSalary().doubleValue())
                    .average()
                    .orElse(0.0);

            long maleCount = people.stream()
                    .filter(p -> p.getGender().toString().equals("MALE"))
                    .count();

            long femaleCount = people.stream()
                    .filter(p -> p.getGender().toString().equals("FEMALE"))
                    .count();

            System.out.printf("Средняя зарплата: %.2f%n", avgSalary);
            System.out.printf("Мужчин: %d (%.1f%%)%n", maleCount,
                    people.isEmpty() ? 0 : (maleCount * 100.0 / people.size()));
            System.out.printf("Женщин: %d (%.1f%%)%n", femaleCount,
                    people.isEmpty() ? 0 : (femaleCount * 100.0 / people.size()));

            // Статистика по подразделениям
            Map<String, ?> departments = csvReaderService.getDepartmentCache();
            System.out.printf("Количество подразделений: %d%n", departments.size());

            System.out.println("\n" + "=".repeat(80));
            System.out.println("СТАТИСТИКА ПО ПОДРАЗДЕЛЕНИЯМ");
            System.out.println("=".repeat(80));

            departments.values().stream()
                    .sorted((d1, d2) -> d1.toString().compareTo(d2.toString()))
                    .forEach(dept -> {
                        long count = people.stream()
                                .filter(p -> p.getDepartment().equals(dept))
                                .count();

                        double avgDeptSalary = people.stream()
                                .filter(p -> p.getDepartment().equals(dept))
                                .mapToDouble(p -> p.getSalary().doubleValue())
                                .average()
                                .orElse(0.0);

                        System.out.printf("%-25s | Сотрудников: %-5d | Средняя ЗП: %10.2f%n",
                                dept, count, avgDeptSalary);
                    });

            // Пример выборки данных
            System.out.println("\n" + "=".repeat(80));
            System.out.println("ПРИМЕРЫ ДАННЫХ (первые 5 записей)");
            System.out.println("=".repeat(80));

            for (int i = 0; i < Math.min(5, people.size()); i++) {
                Person person = people.get(i);
                System.out.printf("%d. %-20s | ID: %-6d | Пол: %-6s | Отдел: %-5s | " +
                                "ЗП: %8.0f | Дата рожд.: %s%n",
                        i + 1,
                        person.getName(),
                        person.getId(),
                        person.getGender(),
                        person.getDepartment().getName(),
                        person.getSalary(),
                        person.getBirthDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            }

            // Дополнительная аналитика
            System.out.println("\n" + "=".repeat(80));
            System.out.println("АНАЛИТИКА");
            System.out.println("=".repeat(80));

            // Самый старый и самый молодой сотрудник
            Person oldest = people.stream()
                    .min((p1, p2) -> p1.getBirthDate().compareTo(p2.getBirthDate()))
                    .orElse(null);

            Person youngest = people.stream()
                    .max((p1, p2) -> p1.getBirthDate().compareTo(p2.getBirthDate()))
                    .orElse(null);

            if (oldest != null && youngest != null) {
                System.out.printf("Самый старший: %s (%s)%n",
                        oldest.getName(), oldest.getBirthDate());
                System.out.printf("Самый младший: %s (%s)%n",
                        youngest.getName(), youngest.getBirthDate());
            }

            // Самая высокая и низкая зарплата
            Person highestPaid = people.stream()
                    .max((p1, p2) -> p1.getSalary().compareTo(p2.getSalary()))
                    .orElse(null);

            Person lowestPaid = people.stream()
                    .min((p1, p2) -> p1.getSalary().compareTo(p2.getSalary()))
                    .orElse(null);

            if (highestPaid != null && lowestPaid != null) {
                System.out.printf("Самая высокая ЗП: %s (%.0f)%n",
                        highestPaid.getName(), highestPaid.getSalary());
                System.out.printf("Самая низкая ЗП: %s (%.0f)%n",
                        lowestPaid.getName(), lowestPaid.getSalary());
            }

        } catch (Exception e) {
            System.err.println("ОШИБКА: " + e.getMessage());
            e.printStackTrace();

            // Советы по устранению проблем
            System.err.println("\n=== ВОЗМОЖНЫЕ ПРИЧИНЫ ОШИБКИ ===");
            System.err.println("1. Файл не найден в src/main/resources/");
            System.err.println("2. Неправильное имя файла");
            System.err.println("3. Неправильная кодировка файла (должна быть UTF-8)");
            System.err.println("4. Неправильный разделитель (должен быть ';')");
            System.err.println("5. Файл поврежден или имеет некорректный формат");
        }
    }
}