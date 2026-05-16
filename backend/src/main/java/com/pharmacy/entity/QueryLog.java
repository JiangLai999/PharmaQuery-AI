package com.pharmacy.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * 查询日志 (MongoDB文档, 用于协同过滤行为分析)
 */
@Data
@Document(collection = "drug_logs")
public class QueryLog {

    @Id
    private String id;

    private String userId;
    private String query;
    private Integer resultCount;
    private Long selectedDrugId;
    private LocalDateTime timestamp;

    public QueryLog() {}

    public QueryLog(String userId, String query, int resultCount, LocalDateTime timestamp) {
        this.userId = userId;
        this.query = query;
        this.resultCount = resultCount;
        this.timestamp = timestamp;
    }
}
