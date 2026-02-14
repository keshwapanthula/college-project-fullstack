package com.admin.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    // In-memory user store for demo (replace with actual database)
    private Map<String, User> users;

    public UserService() {
        // Initialize empty map, actual initialization in @PostConstruct
    }

    @PostConstruct
    private void initializeUsers() {
        users = new HashMap<>();
        
        // Admin user
        users.put("admin", new User("admin", 
            passwordEncoder.encode("admin123"), 
            Arrays.asList("ROLE_ADMIN")));
        
        // Faculty user
        users.put("faculty", new User("faculty", 
            passwordEncoder.encode("faculty123"), 
            Arrays.asList("ROLE_FACULTY")));
        
        // Student user
        users.put("student", new User("student", 
            passwordEncoder.encode("student123"), 
            Arrays.asList("ROLE_STUDENT")));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        
        return new CustomUserPrincipal(user);
    }

    public User findByUsername(String username) {
        return users.get(username);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // Inner User class
    public static class User {
        private String username;
        private String password;
        private List<String> roles;

        public User(String username, String password, List<String> roles) {
            this.username = username;
            this.password = password;
            this.roles = roles;
        }

        // Getters
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public List<String> getRoles() { return roles; }
    }

    // Custom UserPrincipal implementation
    public static class CustomUserPrincipal implements UserDetails {
        private User user;

        public CustomUserPrincipal(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return user.getRoles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        @Override
        public String getPassword() {
            return user.getPassword();
        }

        @Override
        public String getUsername() {
            return user.getUsername();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        public List<String> getRoles() {
            return user.getRoles();
        }
    }
}