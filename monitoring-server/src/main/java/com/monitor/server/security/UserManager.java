package com.monitor.server.security;

import com.monitor.model.User;
import com.monitor.model.User.Role;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages users with JSON persistence and authentication.
 */
public class UserManager {
    
    private static final String USERS_FILE = "data/users.json";
    private static UserManager instance;
    
    private final Map<String, User> users;
    private final Map<String, String> sessions; // token -> username
    
    private UserManager() {
        this.users = new ConcurrentHashMap<>();
        this.sessions = new ConcurrentHashMap<>();
        loadUsers();
        
        // Create default admin if no users exist
        if (users.isEmpty()) {
            createDefaultAdmin();
        }
    }
    
    /**
     * Get singleton instance.
     */
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }
    
    /**
     * Create default admin user.
     */
    private void createDefaultAdmin() {
        User admin = new User("admin", hashPassword("admin123"), Role.ADMIN);
        users.put(admin.getUsername(), admin);
        saveUsers();
        System.out.println("[UserManager] Created default admin user (username: admin, password: admin123)");
    }
    
    /**
     * Authenticate user and return session token.
     */
    public String authenticate(String username, String password) {
        User user = users.get(username);
        
        if (user == null) {
            System.out.println("[UserManager] Authentication failed: user not found - " + username);
            return null;
        }
        
        String passwordHash = hashPassword(password);
        if (!user.getPasswordHash().equals(passwordHash)) {
            System.out.println("[UserManager] Authentication failed: wrong password - " + username);
            return null;
        }
        
        // Generate session token
        String token = generateToken();
        sessions.put(token, username);
        
        // Update last login
        user.setLastLoginAt(System.currentTimeMillis());
        saveUsers();
        
        System.out.println("[UserManager] User authenticated: " + username);
        return token;
    }
    
    /**
     * Get user by session token.
     */
    public User getUserByToken(String token) {
        if (token == null) return null;
        
        String username = sessions.get(token);
        if (username == null) return null;
        
        return users.get(username);
    }
    
    /**
     * Logout user.
     */
    public void logout(String token) {
        String username = sessions.remove(token);
        if (username != null) {
            System.out.println("[UserManager] User logged out: " + username);
        }
    }
    
    /**
     * Create a new user (admin only).
     */
    public boolean createUser(String token, String username, String password, Role role) {
        User admin = getUserByToken(token);
        
        if (admin == null || !admin.isAdmin()) {
            System.out.println("[UserManager] Create user denied: not admin");
            return false;
        }
        
        if (users.containsKey(username)) {
            System.out.println("[UserManager] Create user failed: username exists - " + username);
            return false;
        }
        
        User newUser = new User(username, hashPassword(password), role);
        users.put(username, newUser);
        saveUsers();
        
        System.out.println("[UserManager] User created: " + newUser);
        return true;
    }
    
    /**
     * Delete a user (admin only).
     */
    public boolean deleteUser(String token, String username) {
        User admin = getUserByToken(token);
        
        if (admin == null || !admin.isAdmin()) {
            return false;
        }
        
        // Prevent deleting self
        if (admin.getUsername().equals(username)) {
            return false;
        }
        
        User removed = users.remove(username);
        if (removed != null) {
            saveUsers();
            System.out.println("[UserManager] User deleted: " + username);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get all users (for admin).
     */
    public List<User> getAllUsers(String token) {
        User admin = getUserByToken(token);
        
        if (admin == null || !admin.isAdmin()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(users.values());
    }
    
    /**
     * Change user password.
     */
    public boolean changePassword(String token, String oldPassword, String newPassword) {
        User user = getUserByToken(token);
        
        if (user == null) {
            return false;
        }
        
        if (!user.getPasswordHash().equals(hashPassword(oldPassword))) {
            return false;
        }
        
        user.setPasswordHash(hashPassword(newPassword));
        saveUsers();
        
        System.out.println("[UserManager] Password changed: " + user.getUsername());
        return true;
    }
    
    /**
     * Hash password using SHA-256.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    /**
     * Generate random session token.
     */
    private String generateToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Save users to JSON file.
     */
    private void saveUsers() {
        try {
            Files.createDirectories(Paths.get("data"));
            
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(USERS_FILE))) {
                writer.write("[\n");
                
                int i = 0;
                for (User u : users.values()) {
                    writer.write(String.format(
                        "  {\"username\":\"%s\",\"passwordHash\":\"%s\",\"role\":\"%s\",\"createdAt\":%d,\"lastLoginAt\":%d}",
                        u.getUsername(), u.getPasswordHash(), u.getRole().name(), u.getCreatedAt(), u.getLastLoginAt()
                    ));
                    
                    if (i < users.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                    i++;
                }
                
                writer.write("]");
            }
            
        } catch (IOException e) {
            System.err.println("[UserManager] Failed to save: " + e.getMessage());
        }
    }
    
    /**
     * Load users from JSON file.
     */
    private void loadUsers() {
        Path path = Paths.get(USERS_FILE);
        
        if (!Files.exists(path)) {
            return;
        }
        
        try {
            String content = Files.readString(path);
            parseUsers(content);
            System.out.println("[UserManager] Loaded " + users.size() + " users");
            
        } catch (IOException e) {
            System.err.println("[UserManager] Failed to load: " + e.getMessage());
        }
    }
    
    /**
     * Parse JSON content to users.
     */
    private void parseUsers(String json) {
        int start = 0;
        while ((start = json.indexOf("{", start)) != -1) {
            int end = json.indexOf("}", start);
            if (end == -1) break;
            
            String obj = json.substring(start, end + 1);
            User user = parseUserObject(obj);
            if (user != null) {
                users.put(user.getUsername(), user);
            }
            
            start = end + 1;
        }
    }
    
    /**
     * Parse a single user JSON object.
     */
    private User parseUserObject(String obj) {
        try {
            String username = extractStringValue(obj, "username");
            String passwordHash = extractStringValue(obj, "passwordHash");
            String roleStr = extractStringValue(obj, "role");
            long createdAt = extractLongValue(obj, "createdAt");
            long lastLoginAt = extractLongValue(obj, "lastLoginAt");
            
            Role role = Role.valueOf(roleStr);
            
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(passwordHash);
            user.setRole(role);
            user.setCreatedAt(createdAt);
            user.setLastLoginAt(lastLoginAt);
            
            return user;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractStringValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        int startQuote = json.indexOf("\"", colonIndex);
        int endQuote = json.indexOf("\"", startQuote + 1);
        
        return json.substring(startQuote + 1, endQuote);
    }
    
    private long extractLongValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) return 0;
        
        int colonIndex = json.indexOf(":", keyIndex);
        int start = colonIndex + 1;
        
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        
        return Long.parseLong(json.substring(start, end));
    }
}
