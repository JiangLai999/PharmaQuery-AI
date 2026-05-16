package com.pharmacy.aspect;

import com.pharmacy.entity.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 操作审计切面 (论文 3.4.2)
 * MongoDB不可用时安全降级为控制台日志
 */
@Slf4j
@Aspect
@Component
public class AuditAspect {

    @Autowired(required = false)
    private MongoTemplate mongoTemplate;

    @Around("@annotation(audit)")
    public Object logOperation(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取当前用户信息
        String userId = "anonymous";
        String role = "UNKNOWN";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            userId = auth.getName();
            role = auth.getAuthorities().stream()
                    .filter(a -> a.getAuthority().startsWith("ROLE_"))
                    .findFirst()
                    .map(a -> a.getAuthority().substring(5))
                    .orElse("UNKNOWN");
        }

        // 获取请求IP
        String ip = "unknown";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
            }
        } catch (Exception e) {
            // ignore
        }

        // 执行目标方法
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            // 记录失败日志
            saveLog(userId, role, audit.value(), joinPoint.getSignature().getName(), ip, "FAILED: " + t.getMessage());
            throw t;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        // 记录成功日志
        saveLog(userId, role, audit.value(), joinPoint.getSignature().getName(), ip,
                "SUCCESS, elapsed=" + elapsed + "ms");

        return result;
    }

    private void saveLog(String userId, String role, String action, String resource, String ip, String detail) {
        if (mongoTemplate != null) {
            try {
                OperationLog opLog = new OperationLog();
                opLog.setUserId(userId);
                opLog.setRole(role);
                opLog.setAction(action);
                opLog.setResource(resource);
                opLog.setIp(ip);
                opLog.setDetail(detail);
                opLog.setTimestamp(LocalDateTime.now());
                mongoTemplate.insert(opLog, "operation_logs");
                return;
            } catch (Exception e) {
                log.warn("MongoDB审计日志写入失败, 降级为控制台: {}", e.getMessage());
            }
        }
        // 降级: 输出到控制台日志
        log.info("[AUDIT] user={}, role={}, action={}, resource={}, ip={}, detail={}",
                userId, role, action, resource, ip, detail);
    }
}
