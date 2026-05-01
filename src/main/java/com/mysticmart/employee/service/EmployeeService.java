package com.mysticmart.employee.service;
import com.mysticmart.auth.dto.AuthDtos;
import com.mysticmart.auth.model.User;
import com.mysticmart.auth.repository.UserRepository;
import com.mysticmart.auth.service.AuthService;
import com.mysticmart.common.exception.AppException;
import com.mysticmart.employee.dto.EmployeeDtos;
import com.mysticmart.employee.model.Employee;
import com.mysticmart.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service @RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public Page<EmployeeDtos.EmployeeResponse> getAll(String search, Pageable pageable) {
        Page<Employee> page = StringUtils.hasText(search)
                ? employeeRepository.findWithSearch(search, pageable)
                : employeeRepository.findAllWithUser(pageable);
        return page.map(this::toResponse);
    }

    @Cacheable(value="employee", key="#id")
    public EmployeeDtos.EmployeeResponse getById(Long id) { return toResponse(findOrThrow(id)); }

    @Transactional @CacheEvict(value="employee", allEntries=true)
    public EmployeeDtos.EmployeeResponse create(EmployeeDtos.CreateRequest req) {
        var rr = new AuthDtos.RegisterRequest();
        rr.setFullName(req.getFullName()); rr.setEmail(req.getEmail());
        rr.setPassword(req.getPassword()); rr.setRole(req.getRole() != null ? req.getRole() : User.Role.STAFF);
        User user = authService.register(rr);
        var emp = Employee.builder().user(user).fullName(req.getFullName()).phone(req.getPhone())
                .department(req.getDepartment()).salary(req.getSalary())
                .hireDate(req.getHireDate()).address(req.getAddress()).build();
        return toResponse(employeeRepository.save(emp));
    }

    @Transactional @CacheEvict(value="employee", allEntries=true)
    public EmployeeDtos.EmployeeResponse update(Long id, EmployeeDtos.UpdateRequest req) {
        Employee e = findOrThrow(id);
        if (StringUtils.hasText(req.getFullName())) e.setFullName(req.getFullName());
        if (req.getPhone() != null) e.setPhone(req.getPhone());
        if (req.getDepartment() != null) e.setDepartment(req.getDepartment());
        if (req.getSalary() != null) e.setSalary(req.getSalary());
        if (req.getHireDate() != null) e.setHireDate(req.getHireDate());
        if (req.getAddress() != null) e.setAddress(req.getAddress());
        return toResponse(employeeRepository.save(e));
    }

    @Transactional @CacheEvict(value="employee", allEntries=true)
    public void updateRole(Long id, User.Role role) {
        Employee e = findOrThrow(id); e.getUser().setRole(role); userRepository.save(e.getUser());
    }

    @Transactional @CacheEvict(value="employee", allEntries=true)
    public void delete(Long id) {
        Employee e = findOrThrow(id);
        employeeRepository.delete(e);
        e.getUser().setActive(false); userRepository.save(e.getUser());
    }

    @Transactional @CacheEvict(value="employee", allEntries=true)
    public void deactivateUser(Long userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        u.setActive(false); userRepository.save(u);
    }

    @Transactional @CacheEvict(value="employee", allEntries=true)
    public void activateUser(Long userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        u.setActive(true); userRepository.save(u);
    }

    private Employee findOrThrow(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new AppException("Employee not found", HttpStatus.NOT_FOUND));
    }
    private EmployeeDtos.EmployeeResponse toResponse(Employee e) {
        return EmployeeDtos.EmployeeResponse.builder().id(e.getId()).userId(e.getUser().getId())
                .fullName(e.getFullName()).email(e.getUser().getEmail()).role(e.getUser().getRole())
                .phone(e.getPhone()).department(e.getDepartment()).salary(e.getSalary())
                .hireDate(e.getHireDate()).address(e.getAddress()).active(e.getUser().isActive())
                .createdAt(e.getCreatedAt()).build();
    }
}
