package com.mishanthrope.queueboard.infrastructure.persistence.employee;

import com.mishanthrope.queueboard.domain.employee.Email;
import com.mishanthrope.queueboard.domain.employee.Employee;
import com.mishanthrope.queueboard.domain.employee.EmployeeRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final SpringDataEmployeeRepository springDataRepository;
    private final EmployeeEntityMapper mapper;

    @Override
    @Transactional
    public Employee save(Employee employee) {
        return mapper.toDomain(springDataRepository.save(mapper.toEntity(employee)));
    }

    @Override
    public Optional<Employee> findByEmail(Email email) {
        return springDataRepository.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return springDataRepository.existsByEmail(email.value());
    }
}
