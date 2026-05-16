package com.pharmacy.repository;

import com.pharmacy.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 操作日志 MongoDB Repository
 */
public interface OperationLogRepository extends MongoRepository<OperationLog, String> {

    Page<OperationLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    Page<OperationLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);
}
