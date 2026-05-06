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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Around("@annotation(com.motherhood.shared.audit.AuditedResource)")
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

            // 3 — Extract who is making the request from Spring SecurityContext
            String performedBy = "anonymous";
            String facilityId = null;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                performedBy = auth.getName();
            }

            // 4 — Extract the first UUID argument as resourceId if present
            String resourceId = extractResourceId(joinPoint.getArgs());

            // 5 — Extract HTTP request details
            String clientIp = "unknown";
            String userAgent = "unknown";
            String path = "unknown";

            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                clientIp = getClientIp(request);
                userAgent = request.getHeader("User-Agent");
                path = request.getRequestURI();
            }

            // 6 — Generate traceId and log asynchronously
            String traceId = UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 16);

            auditService.log(action, resourceType, resourceId,
                    performedBy, facilityId,
                    clientIp, userAgent,
                    path, traceId);

        } catch (Exception e) {
            // Never let audit logging crash the main request
            log.error("AuditAspect failed to log: {}", e.getMessage());
        }

        return result;
    }

    // Extracts the first UUID from method arguments — usually the resource ID
    private String extractResourceId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof UUID) return arg.toString();
            if (arg instanceof String str) {
                try {
                    UUID.fromString(str);
                    return str;
                } catch (IllegalArgumentException ignored) {}
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
