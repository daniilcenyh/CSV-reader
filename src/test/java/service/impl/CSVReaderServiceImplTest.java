package service.impl;

import models.Department;
import models.Person;
import models.enums.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CSVReaderServiceImplTest {

    private CSVReaderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CSVReaderServiceImpl();
    }

    @Test
    void testReadPeopleFromCSV_SuccessfulParsing() throws IOException {
        List<Person> people = service.readPeopleFromCSV("people.csv");

        assertEquals(25898, people.size());

        // Verify first person
        Person p1 = people.get(0);
        assertEquals(28281L, p1.getId());
        assertEquals("Aahan", p1.getName());
        assertEquals(Gender.MALE, p1.getGender());
        assertEquals(LocalDate.of(1970, 5, 15), p1.getBirthDate());
        assertEquals("Отдел I", p1.getDepartment().getName());
        assertEquals(new BigDecimal("4800"), p1.getSalary());

        // Verify second person
        Person p2 = people.get(1);
        assertEquals(28282L, p2.getId());
        assertEquals("Aala", p2.getName());
        assertEquals(Gender.FEMALE, p2.getGender());
        assertEquals(LocalDate.of(1983, 2, 7), p2.getBirthDate());
        assertEquals("Отдел J", p2.getDepartment().getName());
        assertEquals(new BigDecimal("2600"), p2.getSalary());

        // Verify last person
        Person last = people.get(people.size() - 1);
        assertEquals(54178L, last.getId());
        assertEquals("Zyta", last.getName());
        assertEquals(Gender.FEMALE, last.getGender());
        assertEquals(LocalDate.of(1955, 4, 16), last.getBirthDate());
        assertEquals("Отдел H", last.getDepartment().getName());
        assertEquals(new BigDecimal("7600"), last.getSalary());
    }

    @Test
    void testGetDepartmentCache_AfterReading() throws IOException {
        service.readPeopleFromCSV("people.csv");
        Map<String, Department> cache = service.getDepartmentCache();

        assertEquals(15, cache.size());
        assertTrue(cache.containsKey("I"));
        assertTrue(cache.containsKey("J"));
        assertTrue(cache.containsKey("F"));
        assertTrue(cache.containsKey("G"));
        assertTrue(cache.containsKey("H"));
        assertTrue(cache.containsKey("C"));
        assertTrue(cache.containsKey("O"));
        assertTrue(cache.containsKey("E"));
        assertTrue(cache.containsKey("B"));
        assertTrue(cache.containsKey("D"));
        assertTrue(cache.containsKey("K"));
        assertTrue(cache.containsKey("A"));
        assertTrue(cache.containsKey("M"));
        assertTrue(cache.containsKey("N"));
        assertTrue(cache.containsKey("L"));

        // Verify a department name
        assertEquals("Отдел A", cache.get("A").getName());
    }

    @Test
    void testReadPeopleFromCSV_FileNotFound_ThrowsIOException() {
        IOException exception = assertThrows(IOException.class, () ->
                service.readPeopleFromCSV("non_existent_file.csv"));

        assertTrue(exception.getMessage().contains("Файл не найден в ресурсах"));
        assertTrue(exception.getMessage().contains("non_existent_file.csv"));

        // Дополнительно: можно проверить cause
        assertTrue(exception.getCause() instanceof FileNotFoundException);
    }
}