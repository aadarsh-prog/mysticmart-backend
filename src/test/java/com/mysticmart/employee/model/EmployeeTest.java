package com.mysticmart.employee.model;



import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmployeeTest {

    private Employee emp;

    @BeforeEach
    void setUp() {
        emp= new Employee();
    }

    @Test
    @DisplayName("should create employee object")
    void testEmployeeObjectCreation() {

        Employee newEmployee = new Employee();

        assertThat(newEmployee).isNotNull();
    }

    @Test
    @DisplayName("should set and get employee id")
    void testIdGetterSetter() {

        Long expectedId = 1L;

        emp.setId(expectedId);

        assertThat(emp.getId()).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("should set and get full name")
    void testFullNameGetterSetter() {

        String expectedName = "Rahul Sharma";

        emp.setFullName(expectedName);

        assertThat(emp.getFullName()).isEqualTo(expectedName);
    }

    @Test
    @DisplayName("should set and get department")
    void testDepartmentGetterSetter() {

        String expectedDepartment = "IT";

        emp.setDepartment(expectedDepartment);

        assertThat(emp.getDepartment()).isEqualTo(expectedDepartment);
    }

    @Test
    @DisplayName("should set and get salary")
    void testSalaryGetterSetter() {

        BigDecimal expectedSalary = new BigDecimal("50000");

        emp.setSalary(expectedSalary);

        assertThat(emp.getSalary()).isEqualTo(expectedSalary);
    }

    @Test
    @DisplayName("should set and get hire date")
    void testHireDateGetterSetter() {

        LocalDate expectedDate = LocalDate.of(2025, 1, 10);

        emp.setHireDate(expectedDate);

        assertThat(emp.getHireDate()).isEqualTo(expectedDate);
    }

    @Test
    @DisplayName("should set and get address")
    void testAddressGetterSetter()
    {

        String expectedAddress = "Delhi";

        emp.setAddress(expectedAddress);

        assertThat(emp.getAddress()).isEqualTo(expectedAddress);
    }
}