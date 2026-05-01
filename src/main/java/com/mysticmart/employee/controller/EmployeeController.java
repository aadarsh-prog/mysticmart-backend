package com.mysticmart.employee.controller;
import com.mysticmart.common.dto.ApiResponse;
import com.mysticmart.employee.dto.EmployeeDtos;
import com.mysticmart.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/employees") @RequiredArgsConstructor
@Tag(name = "Employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Page<EmployeeDtos.EmployeeResponse>>> getAll(
            @RequestParam(required=false) String search,
            @PageableDefault(size=20, sort="fullName") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getAll(search, pageable)));
    }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(id)));
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> create(@Valid @RequestBody EmployeeDtos.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Employee created", employeeService.create(req)));
    }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> update(@PathVariable Long id, @Valid @RequestBody EmployeeDtos.UpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.update(id, req)));
    }
    @PatchMapping("/{id}/role") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateRole(@PathVariable Long id, @RequestBody EmployeeDtos.RoleRequest req) {
        employeeService.updateRole(id, req.getRole());
        return ResponseEntity.ok(ApiResponse.success("Role updated", null));
    }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted", null));
    }
    @DeleteMapping("/users/{userId}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long userId) {
        employeeService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", null));
    }
    @PatchMapping("/users/{userId}/activate") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long userId) {
        employeeService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User activated", null));
    }
}
