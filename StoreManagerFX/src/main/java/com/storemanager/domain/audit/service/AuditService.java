package com.storemanager.domain.audit.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.runtime.async.AuditActorContext;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.model.AuditLog;
import com.storemanager.domain.audit.model.AuditLogFilter;
import com.storemanager.domain.audit.model.AuditLogViewDto;
import com.storemanager.domain.audit.repository.AuditLogRepository;
import com.storemanager.domain.audit.repository.AuditLogFileRepository;
import com.storemanager.domain.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AuditService {

    private static final Gson CHANGE_JSON = new GsonBuilder().serializeNulls().create();

    public boolean recordChange(String module, String action, String entityType, Long entityId,
                                boolean success, String reason, Map<String, Object> before,
                                Map<String, Object> after, Map<String, Object> attempted) {
        return recordEvent(module, action, entityType, entityId, success, reason,
                CHANGE_JSON.toJson(AuditSnapshots.fields(
                        "before", before, "after", after, "attempted", attempted)));
    }

    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String ACTION_ATTENDANCE_SELF_CHECK_IN = "ATTENDANCE_SELF_CHECK_IN";
    public static final String ACTION_ATTENDANCE_SELF_CHECK_OUT = "ATTENDANCE_SELF_CHECK_OUT";
    public static final String ACTION_INVENTORY_ADJUSTMENT = "INVENTORY_ADJUSTMENT";
    public static final String ACTION_IMPORT_FINALIZED = "IMPORT_FINALIZED";
    public static final String ACTION_SALE_FINALIZED = "SALE_FINALIZED";
    public static final String ACTION_BACKUP = "BACKUP";
    public static final String ACTION_RESTORE = "RESTORE";
    public static final String ACTION_DATABASE_RESET = "DATABASE_RESET";

    private final AuditLogRepository auditLogRepository =
            new AuditLogRepository();

    private final AuditLogFileRepository auditLogFileRepository =
            new AuditLogFileRepository();

    public boolean record(String action, String entityType, Long entityId, String details) {

        return recordEvent(moduleFromEntity(entityType), action, entityType, entityId, true, null, details);
    }

    public boolean recordEvent(String module, String action, String targetType, Long targetId, boolean success, String reason, String detailsJson) {

        if (action == null || action.trim().isEmpty()) {
            throw new RuntimeException("Audit action is required");
        }

        if (targetType == null || targetType.trim().isEmpty()) {
            throw new RuntimeException("Audit entity type is required");
        }

        AuditLog log =
                new AuditLog();

        AuditActorContext.Actor actor = AuditActorContext.capture();
        log.setUserId(actor.userId());
        log.setActorUsername(clean(actor.username()));
        log.setModule(clean(module == null ? moduleFromEntity(targetType) : module));
        log.setAction(action.trim());
        log.setEntityType(targetType.trim());
        log.setEntityId(targetId);
        log.setSuccess(success);
        log.setReason(clean(reason));
        log.setDetailsJson(clean(detailsJson));
        log.setDetails(clean(detailsJson));
        log.setCreatedAt(LocalDateTime.now());

        // Persist to text independently, even if the database write subsequently fails.
        // The return value retains its existing meaning: database persistence succeeded.
        auditLogFileRepository.save(log);
        return auditLogRepository.save(log);
    }

    public void recordPermissionDenied(String action, String entityType, Long entityId, String details) {

        recordPermissionDenied(action, entityType, entityId, details, null);
    }

    public void recordPermissionDenied(String action, String entityType, Long entityId, String details, String requiredRole) {

        String attemptedAction =
                action == null || action.isBlank()
                        ? ACTION_PERMISSION_DENIED
                        : action.trim();

        recordEvent("SECURITY", ACTION_PERMISSION_DENIED, entityType == null ? "SECURITY" : entityType, entityId, false, details, "{\"attempted_action\":\"" + escape(attemptedAction)
                        + "\",\"required_role\":\"" + escape(requiredRole)
                        + "\"}");
    }

    public void recordLogin(
            User user
    ) {

        if (user == null) {
            return;
        }

        AppSession.setCurrentUser(user);
        recordEvent(
                "AUTHENTICATION",
                ACTION_LOGIN_SUCCESS,
                "USER",
                user.getId(),
                true,
                null,
                "{\"username\":\"" + escape(user.getUsername()) + "\"}"
        );
    }

    public void recordLoginFailure(
            String username,
            String reason
    ) {

        recordEvent(
                "AUTHENTICATION",
                ACTION_LOGIN_FAILED,
                "USER",
                null,
                false,
                reason,
                "{\"username\":\"" + escape(username) + "\"}"
        );
    }

    public void recordLogout(
            User user
    ) {

        if (user == null) {
            return;
        }

        recordEvent(
                "AUTHENTICATION",
                ACTION_LOGOUT,
                "USER",
                user.getId(),
                true,
                null,
                "{\"username\":\"" + escape(user.getUsername()) + "\"}"
        );
    }

    public List<AuditLogViewDto> findAuditLogs(
            AuditLogFilter filter
    ) {

        validateAccess();

        return auditLogRepository.findAll(filter);
    }

    public void recordDatabaseReset(
            String details
    ) {

        record(ACTION_DATABASE_RESET, "DATABASE", null, details);
    }

    private void validateAccess() {

        if (!PermissionGuard.canViewAuditLogs()) {
            recordPermissionDenied(ACTION_PERMISSION_DENIED, "AUDIT_LOG", null, "Audit log access denied");
            throw new RuntimeException("Audit log access denied");
        }
    }

    private String clean(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private String moduleFromEntity(
            String entityType
    ) {

        if (entityType == null || entityType.isBlank()) {
            return "GENERAL";
        }

        return entityType.trim().toUpperCase();
    }

    private String escape(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
