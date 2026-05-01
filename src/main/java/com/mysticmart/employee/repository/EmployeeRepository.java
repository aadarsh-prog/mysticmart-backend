package com.mysticmart.employee.repository;
import com.mysticmart.employee.model.Employee;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    @Query(value = "SELECT e FROM Employee e JOIN FETCH e.user u WHERE " +
        "(:s IS NULL OR LOWER(e.fullName) LIKE LOWER(CONCAT('%',:s,'%')) " +
        "OR LOWER(COALESCE(e.department,'')) LIKE LOWER(CONCAT('%',:s,'%')) " +
        "OR LOWER(u.email) LIKE LOWER(CONCAT('%',:s,'%')))",
        countQuery = "SELECT COUNT(e) FROM Employee e JOIN e.user u WHERE " +
        "(:s IS NULL OR LOWER(e.fullName) LIKE LOWER(CONCAT('%',:s,'%')) " +
        "OR LOWER(COALESCE(e.department,'')) LIKE LOWER(CONCAT('%',:s,'%')) " +
        "OR LOWER(u.email) LIKE LOWER(CONCAT('%',:s,'%')))")
    Page<Employee> findWithSearch(@Param("s") String search, Pageable pageable);

    @Query(value = "SELECT e FROM Employee e JOIN FETCH e.user u",
           countQuery = "SELECT COUNT(e) FROM Employee e")
    Page<Employee> findAllWithUser(Pageable pageable);

    Optional<Employee> findByUserId(Long userId);
}
