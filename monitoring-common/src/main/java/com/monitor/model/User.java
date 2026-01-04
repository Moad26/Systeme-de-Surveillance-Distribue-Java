package com.monitor.model;

import java.io.Serializable;

/**
 * User model for authentication and authorization.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * User roles with different access levels.
     */
    public enum Role {
        ADMIN, // Full access: manage users, configure alerts, export data
        OPERATOR, // Can configure alerts, view all data, export
        VIEWER // Read-only access to dashboard
    }

    private String username;
    private String passwordHash;
    private Role role;
    private long createdAt;
    private long lastLoginAt;

    public User() {
    }

    public User(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * Check if user has admin privileges.
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Check if user can modify configurations.
     */
    public boolean canConfigure() {
        return role == Role.ADMIN || role == Role.OPERATOR;
    }

    @Override
    public String toString() {
        return String.format("User[%s, role=%s]", username, role);
    }
}
