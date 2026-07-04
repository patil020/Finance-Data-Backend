package com.financeapp.common;

/**
 * DESIGN PATTERN: Constants Pattern
 * Centralized constants for all microservices
 */
public class Constants {
    
    // Kafka Topics
    public static final String KAFKA_RECORD_CREATED_TOPIC = "financial-record-created";
    public static final String KAFKA_RECORD_UPDATED_TOPIC = "financial-record-updated";
    public static final String KAFKA_RECORD_DELETED_TOPIC = "financial-record-deleted";
    public static final String KAFKA_RECORD_DEAD_LETTER_TOPIC = "financial-record-dead-letter";
    public static final String KAFKA_USER_CREATED_TOPIC = "user-created";
    public static final String KAFKA_USER_ROLE_CHANGED_TOPIC = "user-role-changed";
    
    // Service Names for Service Discovery
    public static final String AUTH_SERVICE = "auth-service";
    public static final String USER_SERVICE = "user-service";
    public static final String RECORD_SERVICE = "record-service";
    public static final String DASHBOARD_SERVICE = "dashboard-service";
    
    // Headers
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    // Cache Keys
    public static final String CACHE_DASHBOARD_SUMMARY = "dashboard:summary:";
    public static final String CACHE_DASHBOARD_CATEGORY_WISE = "dashboard:category-wise:";
    
    // Error Codes
    public static final String ERR_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ERR_FORBIDDEN = "FORBIDDEN";
    public static final String ERR_NOT_FOUND = "NOT_FOUND";
    public static final String ERR_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String ERR_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String ERR_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    
    // Saga Status
    public static final String SAGA_STATUS_PENDING = "PENDING";
    public static final String SAGA_STATUS_APPROVED = "APPROVED";
    public static final String SAGA_STATUS_REJECTED = "REJECTED";
    public static final String SAGA_STATUS_COMPENSATED = "COMPENSATED";
}
