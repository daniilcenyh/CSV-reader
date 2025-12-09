package models;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private Long id;

    @NotBlank(message = "Название подразделения не может быть пустым")
    private String name;

    public Department(String name) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.name = name;
    }

    // Переопределяем equals и hashCode для корректной работы с коллекциями
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Department that = (Department) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}