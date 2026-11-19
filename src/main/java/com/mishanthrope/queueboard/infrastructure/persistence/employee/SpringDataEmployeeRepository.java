package com.mishanthrope.queueboard.infrastructure.persistence.employee;

import com.mishanthrope.queueboard.application.auth.query.EmployeeView;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataEmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    Optional<EmployeeEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select new com.mishanthrope.queueboard.application.auth.query.EmployeeView("
            + "e.id, e.email, e.fullName) from EmployeeEntity e where e.id = :id")
    Optional<EmployeeView> findViewById(@Param("id") Long id);
}
