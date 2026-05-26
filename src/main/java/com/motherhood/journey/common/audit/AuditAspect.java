package com.motherhood.journey.common.audit;

import com.motherhood.journey.common.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;

    @Around("@annotation(com.motherhood.journey.common.audit.AuditedResource)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {

        // 1 — Run the actual method first
        Object result = joinPoint.proceed();

        try {
            // 2 — Read the annotation details
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            AuditedResource annotation = method.getAnnotation(AuditedResource.class);

            String action = annotation.action();
            String resourceType = annotation.resourceType();

            // 3 — Caller identity is resolved inside auditService via
            //      SecurityContextHolder, so we do not need to read it here.

            // 4 — Extract the first UUID argument as resourceId if present
            String resourceId = extractResourceId(joinPoint.getArgs());

            // 5 — Extract HTTP request details
            String clientIp = "unknown";
            String userAgent = "unknown";

            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                clientIp = getClientIp(request);
                userAgent = request.getHeader("User-Agent");
            }

            UUID resourceUuid = null;
            if (resourceId != null) {
                try {
                    resourceUuid = UUID.fromString(resourceId);
                } catch (IllegalArgumentException ignored) {
                }
            }
            com.motherhood.journey.common.enums.AuditAction auditAction;
            try {
                auditAction = com.motherhood.journey.common.enums.AuditAction.valueOf(action);
            } catch (IllegalArgumentException e) {
                auditAction = com.motherhood.journey.common.enums.AuditAction.READ;
            }
            auditService.log(auditAction, resourceType, resourceUuid, clientIp, userAgent);

        } catch (Exception e) {
            // Never let audit logging crash the main request
            log.error("AuditAspect failed to log: {}", e.getMessage(), e);
        }

        return result;
    }

    // Extracts the first UUID from method arguments — usually the resource ID
    private String extractResourceId(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return arg.toString();
            }
            if (arg instanceof String str) {
                try {
                    UUID.fromString(str);
                    return str;
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }

    // Handles proxies and load balancers correctly
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
