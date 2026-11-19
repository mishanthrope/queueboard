package com.mishanthrope.queueboard.application.auth.query;

import java.util.Optional;

public interface EmployeeReadRepository {

    Optional<EmployeeView> findById(long id);
}
