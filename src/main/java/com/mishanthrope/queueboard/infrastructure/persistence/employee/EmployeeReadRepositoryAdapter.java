package com.mishanthrope.queueboard.infrastructure.persistence.employee;

import com.mishanthrope.queueboard.application.auth.query.EmployeeReadRepository;
import com.mishanthrope.queueboard.application.auth.query.EmployeeView;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EmployeeReadRepositoryAdapter implements EmployeeReadRepository {

    private final SpringDataEmployeeRepository springDataRepository;

    @Override
    public Optional<EmployeeView> findById(long id) {
        return springDataRepository.findViewById(id);
    }
}
