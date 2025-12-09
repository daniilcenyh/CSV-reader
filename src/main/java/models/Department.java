package models;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Представляет подразделение (отдел) организации.
 * <p>
 * Каждый объект {@code Department} имеет уникальный автоматически генерируемый идентификатор
 * и название, которое не может быть пустым. Подразделения кэшируются по коду подразделения
 * и сравниваются между собой по ID.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    /** Уникальный идентификатор подразделения, генерируется автоматически */
    private Long id;

    /**
     * Название подразделения.
     * Не может быть пустым или состоять только из пробелов.
     */
    @NotBlank(message = "Название подразделения не может быть пустым")
    private String name;

    /**
     * Создаёт новое подразделение с указанным названием.
     * ID присваивается автоматически с помощью атомарного счётчика.
     *
     * @param name название подразделения
     */
    public Department(String name) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = name;
    }

    /**
     * Сравнивает подразделения по их идентификатору.
     *
     * @param o объект для сравнения
     * @return true, если идентификаторы совпадают
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Department that = (Department) o;
        return id != null && id.equals(that.id);
    }

    /**
     * Возвращает хэш-код на основе ID подразделения.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}