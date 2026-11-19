package com.mishanthrope.queueboard.core.infrastructure.persistence.employee;

import com.mishanthrope.queueboard.core.domain.employee.Email;
import com.mishanthrope.queueboard.core.domain.employee.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeEntityMapper {

    public Employee toDomain(EmployeeEntity entity) {
        return Employee.reconstitute(
                entity.getId(),
                new Email(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getFullName());
    }

    public EmployeeEntity toEntity(Employee employee) {
        var entity = new EmployeeEntity();
        entity.setId(employee.getId());
        entity.setEmail(employee.getEmail().value());
        entity.setPasswordHash(employee.getPasswordHash());
        entity.setFullName(employee.getFullName());
        return entity;
    }
}
