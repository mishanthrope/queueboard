package com.mishanthrope.queueboard.core.application.auth.query;

import com.mishanthrope.queueboard.core.application.common.QueryHandler;
import com.mishanthrope.queueboard.core.domain.employee.EmployeeNotFoundError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetEmployeeHandler implements QueryHandler<GetEmployee, EmployeeView> {

    private final EmployeeReadRepository employeeReadRepository;

    @Override
    public EmployeeView handle(GetEmployee query) {
        return employeeReadRepository.findById(query.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundError(query.employeeId()));
    }
}
