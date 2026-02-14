package com.college.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLConnectionTest {
    
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3307/college_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String username = "root";
        String password = "admin123";
        
        System.out.println("========================================");
        System.out.println("MySQL Connection Test for College System");
        System.out.println("========================================");
        
        try {
            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✓ MySQL Driver loaded successfully");
            
            // Test connection
            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("✓ Database connection established successfully!");
            
            // Test database access
            String query = "SELECT DATABASE() as current_db, COUNT(*) as table_count FROM information_schema.tables WHERE table_schema='college_system'";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            if (rs.next()) {
                System.out.println("✓ Current Database: " + rs.getString("current_db"));
                System.out.println("✓ Table Count: " + rs.getInt("table_count"));
            }
            
            // Test table access
            query = "SELECT 'Connection Test' as status, COUNT(*) as student_count FROM students LIMIT 1";
            rs = stmt.executeQuery(query);
            if (rs.next()) {
                System.out.println("✓ Table Access: " + rs.getString("status"));
                System.out.println("✓ Student Records: " + rs.getInt("student_count"));
            }
            
            connection.close();
            System.out.println("✓ Connection closed successfully");
            
            System.out.println("\n========================================");
            System.out.println("🎉 MYSQL INTEGRATION TEST PASSED!");
            System.out.println("========================================");
            System.out.println("Your Spring Boot app is ready to connect to MySQL!");
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
        }
    }
}