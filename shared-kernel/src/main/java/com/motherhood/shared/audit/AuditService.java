package com.motherhood.shared.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);


    public void log(String action, String detail, String path, String traceId) {
        log.debug("[AUDIT STUB] action={} | detail={} | path={} | traceId={}",
                action, detail, path, traceId);
    }
}
