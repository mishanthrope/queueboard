package com.mishanthrope.queueboard.application.auth.query;

import com.mishanthrope.queueboard.application.common.QueryHandler;
import com.mishanthrope.queueboard.domain.employee.EmployeeNotFoundError;
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
