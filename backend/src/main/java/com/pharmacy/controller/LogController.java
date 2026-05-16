package com.pharmacy.controller;

import com.pharmacy.dto.ApiResult;
import com.pharmacy.entity.OperationLog;
import com.pharmacy.repository.OperationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 操作日志控制器 (论文 3.4.2)
 * MongoDB不可用时返回空数据
 */
@RestController
@RequestMapping("/logs")
public class LogController {

    @Autowired(required = false)
    private OperationLogRepository logRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('LOG_READ')")
    public ApiResult<Page<OperationLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (logRepository == null) {
            return ApiResult.success(new PageImpl<>(Collections.emptyList()));
        }
        Page<OperationLog> logs = logRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
        return ApiResult.success(logs);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('LOG_READ')")
    public ApiResult<Page<OperationLog>> getLogsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (logRepository == null) {
            return ApiResult.success(new PageImpl<>(Collections.emptyList()));
        }
        Page<OperationLog> logs = logRepository.findByUserIdOrderByTimestampDesc(
                userId, PageRequest.of(page, size));
        return ApiResult.success(logs);
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasAuthority('LOG_READ')")
    public ApiResult<Page<OperationLog>> getLogsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (logRepository == null) {
            return ApiResult.success(new PageImpl<>(Collections.emptyList()));
        }
        Page<OperationLog> logs = logRepository.findByActionOrderByTimestampDesc(
                action, PageRequest.of(page, size));
        return ApiResult.success(logs);
    }
}
