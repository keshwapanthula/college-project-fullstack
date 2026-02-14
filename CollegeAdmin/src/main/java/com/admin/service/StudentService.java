package com.admin.service;

import com.admin.entity.Student;
import com.admin.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    // Create operations
    public Student createStudent(Student student) {
        validateStudent(student);
        return studentRepository.save(student);
    }

    public List<Student> createStudents(List<Student> students) {
        students.forEach(this::validateStudent);
        return studentRepository.saveAll(students);
    }

    // Read operations
    @Transactional(readOnly = true)
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Student> getAllStudents(Sort sort) {
        return studentRepository.findAll(sort);
    }

    @Transactional(readOnly = true)
    public Page<Student> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Student> getStudentByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public Optional<Student> getStudentByEmail(String email) {
        return studentRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsByDepartment(String department) {
        return studentRepository.findByDepartment(department);
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsByStatus(Student.Status status) {
        return studentRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsByYear(Integer year) {
        return studentRepository.findByYear(year);
    }

    @Transactional(readOnly = true)
    public List<Student> searchStudentsByName(String name) {
        return studentRepository.findByFullNameContaining(name);
    }

    @Transactional(readOnly = true)
    public List<Student> getActiveStudentsByDepartment(String department) {
        return studentRepository.findActiveStudentsByDepartment(department, Student.Status.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Long countActiveStudentsByDepartment(String department) {
        return studentRepository.countActiveStudentsByDepartment(department);
    }

    @Transactional(readOnly = true)
    public List<String> getAllActiveDepartments() {
        return studentRepository.findAllActiveDepartments();
    }

    @Transactional(readOnly = true)
    public List<Student> getRecentlyAddedStudents(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return studentRepository.findRecentlyAdded(fromDate);
    }

    // Update operations
    public Student updateStudent(Long id, Student updatedStudent) {
        return studentRepository.findById(id)
                .map(student -> {
                    updateStudentFields(student, updatedStudent);
                    validateStudent(student);
                    return studentRepository.save(student);
                })
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    public Student updateStudentByStudentId(String studentId, Student updatedStudent) {
        return studentRepository.findByStudentId(studentId)
                .map(student -> {
                    updateStudentFields(student, updatedStudent);
                    validateStudent(student);
                    return studentRepository.save(student);
                })
                .orElseThrow(() -> new RuntimeException("Student not found with studentId: " + studentId));
    }

    public Student updateStudentStatus(Long id, Student.Status status) {
        return studentRepository.findById(id)
                .map(student -> {
                    student.setStatus(status);
                    return studentRepository.save(student);
                })
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }

    // Delete operations
    public void deleteStudent(Long id) {
        if (studentRepository.existsById(id)) {
            studentRepository.deleteById(id);
        } else {
            throw new RuntimeException("Student not found with id: " + id);
        }
    }

    public void deleteStudentByStudentId(String studentId) {
        if (studentRepository.existsByStudentId(studentId)) {
            studentRepository.deleteByStudentId(studentId);
        } else {
            throw new RuntimeException("Student not found with studentId: " + studentId);
        }
    }

    // Utility methods
    @Transactional(readOnly = true)
    public boolean existsByStudentId(String studentId) {
        return studentRepository.existsByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return studentRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public long getTotalStudentCount() {
        return studentRepository.count();
    }

    // Bulk operations
    public List<Student> updateStudentsStatus(List<Long> studentIds, Student.Status status) {
        List<Student> students = studentRepository.findAllById(studentIds);
        students.forEach(student -> student.setStatus(status));
        return studentRepository.saveAll(students);
    }

    public void deleteStudents(List<Long> studentIds) {
        studentRepository.deleteAllById(studentIds);
    }

    // Private helper methods
    private void updateStudentFields(Student existingStudent, Student updatedStudent) {
        if (updatedStudent.getFirstName() != null) {
            existingStudent.setFirstName(updatedStudent.getFirstName());
        }
        if (updatedStudent.getLastName() != null) {
            existingStudent.setLastName(updatedStudent.getLastName());
        }
        if (updatedStudent.getEmail() != null) {
            existingStudent.setEmail(updatedStudent.getEmail());
        }
        if (updatedStudent.getDateOfBirth() != null) {
            existingStudent.setDateOfBirth(updatedStudent.getDateOfBirth());
        }
        if (updatedStudent.getDepartment() != null) {
            existingStudent.setDepartment(updatedStudent.getDepartment());
        }
        if (updatedStudent.getMajor() != null) {
            existingStudent.setMajor(updatedStudent.getMajor());
        }
        if (updatedStudent.getYear() != null) {
            existingStudent.setYear(updatedStudent.getYear());
        }
        if (updatedStudent.getPhoneNumber() != null) {
            existingStudent.setPhoneNumber(updatedStudent.getPhoneNumber());
        }
        if (updatedStudent.getAddress() != null) {
            existingStudent.setAddress(updatedStudent.getAddress());
        }
        if (updatedStudent.getStatus() != null) {
            existingStudent.setStatus(updatedStudent.getStatus());
        }
    }

    private void validateStudent(Student student) {
        if (student.getStudentId() != null && !student.getStudentId().equals("")) {
            // Check for duplicate studentId (excluding current student)
            Optional<Student> existingByStudentId = studentRepository.findByStudentId(student.getStudentId());
            if (existingByStudentId.isPresent() && !existingByStudentId.get().getId().equals(student.getId())) {
                throw new RuntimeException("Student ID already exists: " + student.getStudentId());
            }
        }

        if (student.getEmail() != null && !student.getEmail().equals("")) {
            // Check for duplicate email (excluding current student)
            Optional<Student> existingByEmail = studentRepository.findByEmail(student.getEmail());
            if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(student.getId())) {
                throw new RuntimeException("Email already exists: " + student.getEmail());
            }
        }
    }
}