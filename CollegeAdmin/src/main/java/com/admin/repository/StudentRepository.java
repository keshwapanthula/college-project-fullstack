package com.admin.repository;

import com.admin.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Custom query methods using method naming conventions
    Optional<Student> findByStudentId(String studentId);
    
    Optional<Student> findByEmail(String email);
    
    List<Student> findByDepartment(String department);
    
    List<Student> findByStatus(Student.Status status);
    
    List<Student> findByYear(Integer year);
    
    List<Student> findByFirstNameContainingIgnoreCase(String firstName);
    
    List<Student> findByLastNameContainingIgnoreCase(String lastName);
    
    List<Student> findByDepartmentAndYear(String department, Integer year);
    
    List<Student> findByDepartmentAndStatus(String department, Student.Status status);
    
    List<Student> findByDateOfBirthBetween(LocalDate startDate, LocalDate endDate);

    // Custom JPQL queries
    @Query("SELECT s FROM Student s WHERE s.firstName LIKE %:name% OR s.lastName LIKE %:name%")
    List<Student> findByFullNameContaining(@Param("name") String name);

    @Query("SELECT s FROM Student s WHERE s.department = :department AND s.status = :status ORDER BY s.lastName, s.firstName")
    List<Student> findActiveStudentsByDepartment(@Param("department") String department, 
                                                @Param("status") Student.Status status);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.department = :department AND s.status = 'ACTIVE'")
    Long countActiveStudentsByDepartment(@Param("department") String department);

    @Query("SELECT DISTINCT s.department FROM Student s WHERE s.status = 'ACTIVE' ORDER BY s.department")
    List<String> findAllActiveDepartments();

    @Query("SELECT s FROM Student s WHERE s.year = :year AND s.status = 'ACTIVE' ORDER BY s.department, s.lastName")
    List<Student> findActiveStudentsByYear(@Param("year") Integer year);

    // Native SQL queries for complex operations
    @Query(value = "SELECT * FROM students WHERE YEAR(date_of_birth) = :birthYear", nativeQuery = true)
    List<Student> findByBirthYear(@Param("birthYear") Integer birthYear);

    @Query(value = "SELECT department, COUNT(*) as student_count FROM students WHERE status = 'ACTIVE' GROUP BY department ORDER BY student_count DESC", 
           nativeQuery = true)
    List<Object[]> getDepartmentWiseStudentCount();

    // Advanced queries
    @Query("SELECT s FROM Student s WHERE s.email LIKE %:domain%")
    List<Student> findByEmailDomain(@Param("domain") String domain);

    @Query("SELECT s FROM Student s WHERE s.createdAt >= :fromDate ORDER BY s.createdAt DESC")
    List<Student> findRecentlyAdded(@Param("fromDate") java.time.LocalDateTime fromDate);

    // Check if student exists
    boolean existsByStudentId(String studentId);
    
    boolean existsByEmail(String email);

    // Delete operations
    void deleteByStudentId(String studentId);
}