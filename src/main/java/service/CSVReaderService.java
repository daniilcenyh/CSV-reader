package service;

import models.Department;
import models.Person;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CSVReaderService {
    List<Person> readPeopleFromCSV(String csvFilePath) throws IOException;
    Map<String, Department> getDepartmentCache();
}
