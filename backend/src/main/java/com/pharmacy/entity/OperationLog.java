package com.pharmacy.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * 操作日志 (MongoDB文档)
 */
@Data
@Document(collection = "operation_logs")
public class OperationLog {

    @Id
    private String id;

    private String userId;
    private String role;
    private String action;
    private String resource;
    private String detail;
    private String ip;
    private LocalDateTime timestamp;
}
