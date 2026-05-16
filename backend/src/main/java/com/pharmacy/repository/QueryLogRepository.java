package com.pharmacy.repository;

import com.pharmacy.entity.QueryLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

/**
 * 查询日志 MongoDB Repository
 */
public interface QueryLogRepository extends MongoRepository<QueryLog, String> {

    List<QueryLog> findByUserIdOrderByTimestampDesc(String userId);
}
