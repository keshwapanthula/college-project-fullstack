package com.admin.service.aws;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminResetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

/**
 * AWS Cognito Authentication Service
 * Provides comprehensive user authentication, authorization, and user management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsCognitoService {

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.user-pool-id:us-east-1_AAAABBBB}")
    private String userPoolId;

    @Value("${aws.cognito.client-id:abcdefghijklmnop}")
    private String clientId;

    @Value("${aws.cognito.client-secret:}")  // Optional for public clients
    private String clientSecret;

    @Value("${aws.cognito.identity-pool-id:us-east-1:00000000-1111-2222-3333-444444444444}")
    private String identityPoolId;

    /**
     * Register a new user in Cognito User Pool
     */
    public CompletableFuture<UserRegistrationResult> registerUser(UserRegistrationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Registering new user: {}", request.getUsername());

                // Set user attributes
                Map<String, String> userAttributes = new HashMap<>();
                userAttributes.put("email", request.getEmail());
                userAttributes.put("name", request.getFullName());
                userAttributes.put("custom:student_id", request.getStudentId());
                userAttributes.put("custom:department", request.getDepartment());
                userAttributes.put("custom:role", request.getRole());

                List<AttributeType> attributes = userAttributes.entrySet().stream()
                        .map(entry -> AttributeType.builder()
                                .name(entry.getKey())
                                .value(entry.getValue())
                                .build())
                        .toList();

                AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(request.getUsername())
                        .userAttributes(attributes)
                        .temporaryPassword(request.getTemporaryPassword())
                        .messageAction(MessageActionType.SUPPRESS) // Don't send welcome email
                        .build();

                AdminCreateUserResponse response = cognitoClient.adminCreateUser(createUserRequest);

                log.info("User created successfully: {}", request.getUsername());

                return UserRegistrationResult.builder()
                        .username(request.getUsername())
                        .userSub(response.user().attributes().stream()
                                .filter(attr -> "sub".equals(attr.name()))
                                .findFirst()
                                .map(AttributeType::value)
                                .orElse(""))
                        .status(response.user().userStatus().toString())
                        .success(true)
                        .build();

            } catch (Exception e) {
                log.error("Failed to register user {}: {}", request.getUsername(), e.getMessage());
                return UserRegistrationResult.builder()
                        .username(request.getUsername())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build();
            }
        });
    }

    /**
     * Authenticate user and get tokens
     */
    public CompletableFuture<AuthenticationResult> authenticateUser(UserAuthenticationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Authenticating user: {}", request.getUsername());

                Map<String, String> authParams = new HashMap<>();
                authParams.put("USERNAME", request.getUsername());
                authParams.put("PASSWORD", request.getPassword());

                AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                        .userPoolId(userPoolId)
                        .clientId(clientId)
                        .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                        .authParameters(authParams)
                        .build();

                AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);

                if (authResponse.authenticationResult() != null) {
                    software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType result = 
                            authResponse.authenticationResult();

                    log.info("Authentication successful for user: {}", request.getUsername());

                    return AuthenticationResult.builder()
                            .accessToken(result.accessToken())
                            .idToken(result.idToken())
                            .refreshToken(result.refreshToken())
                            .tokenType(result.tokenType())
                            .expiresIn(result.expiresIn())
                            .success(true)
                            .build();
                } else {
                    // Handle challenges (e.g., NEW_PASSWORD_REQUIRED)
                    String challengeName = authResponse.challengeName() != null ? 
                            authResponse.challengeName().toString() : "UNKNOWN";

                    log.warn("Authentication challenge required: {} for user: {}", challengeName, request.getUsername());

                    return AuthenticationResult.builder()
                            .success(false)
                            .challengeName(challengeName)
                            .challengeParameters(authResponse.challengeParameters())
                            .session(authResponse.session())
                            .build();
                }

            } catch (Exception e) {
                log.error("Failed to authenticate user {}: {}", request.getUsername(), e.getMessage());
                return AuthenticationResult.builder()
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build();
            }
        });
    }

    /**
     * Respond to authentication challenges (e.g., new password required)
     */
    public CompletableFuture<AuthenticationResult> respondToChallenge(ChallengeResponse challengeResponse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Responding to auth challenge: {} for user: {}", 
                        challengeResponse.getChallengeName(), challengeResponse.getUsername());

                AdminRespondToAuthChallengeRequest challengeRequest = AdminRespondToAuthChallengeRequest.builder()
                        .userPoolId(userPoolId)
                        .clientId(clientId)
                        .challengeName(ChallengeNameType.fromValue(challengeResponse.getChallengeName()))
                        .session(challengeResponse.getSession())
                        .challengeResponses(challengeResponse.getChallengeResponses())
                        .build();

                AdminRespondToAuthChallengeResponse response = cognitoClient.adminRespondToAuthChallenge(challengeRequest);

                if (response.authenticationResult() != null) {
                    software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType result = 
                            response.authenticationResult();

                    log.info("Challenge response successful for user: {}", challengeResponse.getUsername());

                    return AuthenticationResult.builder()
                            .accessToken(result.accessToken())
                            .idToken(result.idToken())
                            .refreshToken(result.refreshToken())
                            .tokenType(result.tokenType())
                            .expiresIn(result.expiresIn())
                            .success(true)
                            .build();
                }

                return AuthenticationResult.builder()
                        .success(false)
                        .errorMessage("Challenge response failed")
                        .build();

            } catch (Exception e) {
                log.error("Failed to respond to challenge for user {}: {}", 
                        challengeResponse.getUsername(), e.getMessage());
                return AuthenticationResult.builder()
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build();
            }
        });
    }

    /**
     * Get user information from access token
     */
    public UserInfo getUserInfo(String accessToken) {
        try {
            GetUserRequest request = GetUserRequest.builder()
                    .accessToken(accessToken)
                    .build();

            GetUserResponse response = cognitoClient.getUser(request);

            Map<String, String> attributes = response.userAttributes().stream()
                    .collect(HashMap::new, 
                            (map, attr) -> map.put(attr.name(), attr.value()),
                            HashMap::putAll);

            log.info("Retrieved user info for: {}", response.username());

            return UserInfo.builder()
                    .username(response.username())
                    .userStatus("CONFIRMED")
                    .email(attributes.get("email"))
                    .name(attributes.get("name"))
                    .studentId(attributes.get("custom:student_id"))
                    .department(attributes.get("custom:department"))
                    .role(attributes.get("custom:role"))
                    .emailVerified(Boolean.parseBoolean(attributes.get("email_verified")))
                    .attributes(attributes)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get user info from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Update user attributes
     */
    public CompletableFuture<Boolean> updateUserAttributes(String username, Map<String, String> attributes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<AttributeType> userAttributes = attributes.entrySet().stream()
                        .map(entry -> AttributeType.builder()
                                .name(entry.getKey())
                                .value(entry.getValue())
                                .build())
                        .toList();

                AdminUpdateUserAttributesRequest request = AdminUpdateUserAttributesRequest.builder()
                        .userPoolId(userPoolId)
                        .username(username)
                        .userAttributes(userAttributes)
                        .build();

                cognitoClient.adminUpdateUserAttributes(request);
                log.info("User attributes updated for: {}", username);
                return true;

            } catch (Exception e) {
                log.error("Failed to update user attributes for {}: {}", username, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Reset user password
     */
    public CompletableFuture<Boolean> resetUserPassword(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AdminResetUserPasswordRequest request = AdminResetUserPasswordRequest.builder()
                        .userPoolId(userPoolId)
                        .username(username)
                        .build();

                cognitoClient.adminResetUserPassword(request);
                log.info("Password reset initiated for user: {}", username);
                return true;

            } catch (Exception e) {
                log.error("Failed to reset password for user {}: {}", username, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Add user to group (for role-based access control)
     */
    public CompletableFuture<Boolean> addUserToGroup(String username, String groupName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AdminAddUserToGroupRequest request = AdminAddUserToGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .username(username)
                        .groupName(groupName)
                        .build();

                cognitoClient.adminAddUserToGroup(request);
                log.info("User {} added to group: {}", username, groupName);
                return true;

            } catch (Exception e) {
                log.error("Failed to add user {} to group {}: {}", username, groupName, e.getMessage());
                return false;
            }
        });
    }

    /**
     * List users in a group
     */
    public List<String> getUsersInGroup(String groupName) {
        try {
            ListUsersInGroupRequest request = ListUsersInGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .groupName(groupName)
                    .build();

            ListUsersInGroupResponse response = cognitoClient.listUsersInGroup(request);
            
            List<String> usernames = response.users().stream()
                    .map(UserType::username)
                    .toList();

            log.info("Found {} users in group: {}", usernames.size(), groupName);
            return usernames;

        } catch (Exception e) {
            log.error("Failed to list users in group {}: {}", groupName, e.getMessage());
            return List.of();
        }
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            // In a real implementation, you would verify JWT signature and expiration
            // This is a simplified version
            GetUserRequest request = GetUserRequest.builder()
                    .accessToken(token)
                    .build();

            cognitoClient.getUser(request);
            return true;

        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get Cognito service health status
     */
    public CompletableFuture<Map<String, Object>> getCognitoHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test connection by describing user pool
                DescribeUserPoolRequest request = DescribeUserPoolRequest.builder()
                        .userPoolId(userPoolId)
                        .build();
                
                cognitoClient.describeUserPool(request);

                return Map.of(
                        "status", "UP",
                        "service", "AWS Cognito",
                        "timestamp", java.time.Instant.now().toString()
                );
            } catch (Exception e) {
                log.error("Cognito health check failed", e);
                return Map.of(
                        "status", "DOWN",
                        "error", e.getMessage(),
                        "timestamp", java.time.Instant.now().toString()
                );
            }
        });
    }

    // DTOs for Cognito operations
    @lombok.Data
    @lombok.Builder
    public static class UserRegistrationRequest {
        private String username;
        private String email;
        private String fullName;
        private String studentId;
        private String department;
        private String role;
        private String temporaryPassword;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserRegistrationResult {
        private String username;
        private String userSub;
        private String status;
        private boolean success;
        private String errorMessage;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserAuthenticationRequest {
        private String username;
        private String password;
    }

    @lombok.Data
    @lombok.Builder
    public static class AuthenticationResult {
        private String accessToken;
        private String idToken;
        private String refreshToken;
        private String tokenType;
        private Integer expiresIn;
        private boolean success;
        private String errorMessage;
        private String challengeName;
        private Map<String, String> challengeParameters;
        private String session;
    }

    @lombok.Data
    @lombok.Builder
    public static class ChallengeResponse {
        private String username;
        private String challengeName;
        private String session;
        private Map<String, String> challengeResponses;
    }

    @lombok.Data
    @lombok.Builder
    public static class UserInfo {
        private String username;
        private String userStatus;
        private String email;
        private String name;
        private String studentId;
        private String department;
        private String role;
        private boolean emailVerified;
        private Map<String, String> attributes;
    }
}