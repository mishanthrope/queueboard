package com.mishanthrope.queueboard.presentation.auth;

import com.mishanthrope.queueboard.domain.employee.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EmployeeResponseMapper {

    @Mapping(target = "email", expression = "java(employee.getEmail().value())")
    EmployeeResponse toResponse(Employee employee);
}
