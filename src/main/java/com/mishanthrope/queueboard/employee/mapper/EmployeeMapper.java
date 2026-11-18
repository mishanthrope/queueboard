package com.mishanthrope.queueboard.employee.mapper;

import com.mishanthrope.queueboard.employee.dto.EmployeeResponse;
import com.mishanthrope.queueboard.employee.dto.RegisterRequest;
import com.mishanthrope.queueboard.employee.model.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmployeeMapper {

    EmployeeResponse toResponse(Employee employee);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    Employee toEntity(RegisterRequest request);
}
