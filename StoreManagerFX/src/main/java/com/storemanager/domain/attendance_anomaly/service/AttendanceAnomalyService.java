package com.storemanager.domain.attendance_anomaly.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.repository.AttendanceRepository;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomaly;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyFilter;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalySeverity;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyStatus;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyType;
import com.storemanager.domain.attendance_anomaly.model.WorkShiftRule;
import com.storemanager.domain.attendance_anomaly.repository.AttendanceAnomalyRepository;
import com.storemanager.domain.attendance_anomaly.repository.WorkShiftRuleRepository;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.model.EmployeeBranchAssignment;
import com.storemanager.domain.branch.repository.BranchRepository;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.repository.EmployeeRepository;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;
import com.storemanager.core.util.TimeFormatUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AttendanceAnomalyService {

    private static final LocalTime DEFAULT_SHIFT_START =
            LocalTime.of(9, 0);

    private static final LocalTime DEFAULT_SHIFT_END =
            LocalTime.of(18, 0);

    private static final int DEFAULT_EARLY_TOLERANCE_MINUTES = 15;

    private static final int DEFAULT_LATE_TOLERANCE_MINUTES = 15;

    private static final BigDecimal DEFAULT_MAX_WORK_HOURS =
            BigDecimal.valueOf(9.0);

    private final AttendanceRepository attendanceRepository =
            new AttendanceRepository();

    private final AttendanceAnomalyRepository anomalyRepository =
            new AttendanceAnomalyRepository();

    private final WorkShiftRuleRepository workShiftRuleRepository =
            new WorkShiftRuleRepository();

    private final BranchRepository branchRepository =
            new BranchRepository();

    private final EmployeeRepository employeeRepository =
            new EmployeeRepository();

    private final UserRepository userRepository =
            new UserRepository();

    private final NotificationService notificationService =
            new NotificationService();

    private final AuditService auditService =
            new AuditService();

    public List<AttendanceAnomaly> findReviewAnomalies(
            AttendanceAnomalyFilter filter
    ) {

        RoleType role =
                getCurrentRole();

        if (role == null) {
            return List.of();
        }

        if (PermissionGuard.isDeveloper()
                || PermissionGuard.isOwner()) {
            return anomalyRepository.findAll(filter);
        }

        if (!PermissionGuard.isManager()) {
            auditService.recordPermissionDenied(
                    AuditService.ACTION_PERMISSION_DENIED,
                    "ATTENDANCE_ANOMALY",
                    null,
                    "Attendance anomaly review denied",
                    null
            );
            throw new RuntimeException("Attendance anomaly access denied");
        }

        List<Long> branchIds =
                getManagerBranchIds();

        if (branchIds.isEmpty()) {
            return List.of();
        }

        AttendanceAnomalyFilter scopedFilter =
                copyFilter(filter);
        scopedFilter.setBranchIds(branchIds);

        if (scopedFilter.getBranchId() != null
                && !branchIds.contains(scopedFilter.getBranchId())) {
            return List.of();
        }

        return anomalyRepository.findAll(scopedFilter);
    }

    public AttendanceAnomaly findAnomalyById(
            Long anomalyId
    ) {

        AttendanceAnomaly anomaly =
                anomalyRepository.findById(anomalyId);

        if (anomaly == null) {
            return null;
        }

        ensureCanReviewAnomaly(anomaly);
        return anomaly;
    }

    public AttendanceSession findAttendanceSessionById(
            Long sessionId
    ) {

        AttendanceSession session =
                attendanceRepository.findSessionById(sessionId);

        if (session == null) {
            return null;
        }

        ensureCanReviewBranch(session.getBranchId());
        return session;
    }

    public List<Employee> findReviewEmployees() {

        if (PermissionGuard.isDeveloper()
                || PermissionGuard.isOwner()) {
            return employeeRepository.findAll()
                    .stream()
                    .filter(Employee::isActive)
                    .sorted(Comparator.comparing(Employee::getFullName,
                            Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();
        }

        if (!PermissionGuard.isManager()) {
            return List.of();
        }

        Set<Long> branchIds =
                getManagerBranchIds()
                        .stream()
                        .collect(Collectors.toSet());

        if (branchIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Employee> employeesById =
                employeeRepository.findAll()
                        .stream()
                        .filter(Employee::isActive)
                        .collect(
                                Collectors.toMap(
                                        Employee::getId,
                                        Function.identity(),
                                        (left, right) -> left
                                )
                        );

        return branchRepository.findAllAssignments()
                .stream()
                .filter(EmployeeBranchAssignment::isActive)
                .filter(assignment -> branchIds.contains(assignment.getBranchId()))
                .map(EmployeeBranchAssignment::getEmployeeId)
                .distinct()
                .map(employeesById::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Employee::getFullName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public List<Branch> findReviewBranches() {

        if (PermissionGuard.isDeveloper()
                || PermissionGuard.isOwner()) {
            return branchRepository.findActiveBranches();
        }

        if (!PermissionGuard.isManager()) {
            return List.of();
        }

        Employee currentEmployee =
                getCurrentEmployee();

        if (currentEmployee == null) {
            return List.of();
        }

        return branchRepository.findActiveBranchesForEmployeeId(
                currentEmployee.getId()
        );
    }

    public boolean submitManagerReport(
            Long anomalyId,
            String reportText
    ) {

        if (!canManageAnomalyReview()) {
            denyReviewAccess(anomalyId, "Manager report denied");
        }

        if (reportText == null || reportText.trim().isEmpty()) {
            throw new RuntimeException("Manager report text is required");
        }

        AttendanceAnomaly anomaly =
                requireAnomaly(anomalyId);

        ensureCanReviewAnomaly(anomaly);

        boolean updated =
                anomalyRepository.updateManagerReport(
                        anomalyId,
                        true,
                        clean(reportText)
                );

        if (updated) {
            anomaly.setManagerReported(true);
            anomaly.setManagerReportText(clean(reportText));
            if (anomaly.getStatus() == null
                    || anomaly.getStatus() == AttendanceAnomalyStatus.OPEN) {
                anomaly.setStatus(AttendanceAnomalyStatus.REPORTED_BY_MANAGER);
            }

            anomalyRepository.updateStatus(
                    anomalyId,
                    AttendanceAnomalyStatus.REPORTED_BY_MANAGER,
                    anomaly.getResolvedByUserId(),
                    anomaly.getResolvedAt()
            );

            auditService.record(
                    "ATTENDANCE_ANOMALY_MANAGER_REPORT",
                    "ATTENDANCE_ANOMALY",
                    anomalyId,
                    "Manager report submitted: " + clean(reportText),
                    anomaly.getBranchId()
            );

            notifyReviewStakeholders(
                    anomaly,
                    "Manager anomaly report",
                    "Manager reported an attendance anomaly: "
                            + anomaly.getMessage()
                            + "\n\nReport: "
                            + clean(reportText)
            );
        }

        return updated;
    }

    public boolean resolveAnomaly(
            Long anomalyId
    ) {

        if (!canAdminReview()) {
            denyReviewAccess(anomalyId, "Resolve anomaly denied");
        }

        AttendanceAnomaly anomaly =
                requireAnomaly(anomalyId);

        ensureCanReviewAnomaly(anomaly);

        Long currentUserId =
                getCurrentUserId();

        boolean updated =
                anomalyRepository.updateStatus(
                        anomalyId,
                        AttendanceAnomalyStatus.RESOLVED,
                        currentUserId,
                        TimeFormatUtil.truncateToSeconds(
                                LocalDateTime.now()
                        )
                );

        if (updated) {
            auditService.record(
                    "ATTENDANCE_ANOMALY_RESOLVED",
                    "ATTENDANCE_ANOMALY",
                    anomalyId,
                    "Attendance anomaly resolved",
                    anomaly.getBranchId()
            );
        }

        return updated;
    }

    public boolean dismissAnomaly(
            Long anomalyId
    ) {

        if (!canAdminReview()) {
            denyReviewAccess(anomalyId, "Dismiss anomaly denied");
        }

        AttendanceAnomaly anomaly =
                requireAnomaly(anomalyId);

        ensureCanReviewAnomaly(anomaly);

        Long currentUserId =
                getCurrentUserId();

        boolean updated =
                anomalyRepository.updateStatus(
                        anomalyId,
                        AttendanceAnomalyStatus.DISMISSED,
                        currentUserId,
                        TimeFormatUtil.truncateToSeconds(
                                LocalDateTime.now()
                        )
                );

        if (updated) {
            auditService.record(
                    "ATTENDANCE_ANOMALY_DISMISSED",
                    "ATTENDANCE_ANOMALY",
                    anomalyId,
                    "Attendance anomaly dismissed",
                    anomaly.getBranchId()
            );
        }

        return updated;
    }

    public boolean updateEmployeeNotification(
            Long anomalyId,
            boolean enabled
    ) {

        if (!canAdminReview()) {
            denyReviewAccess(anomalyId, "Employee notification update denied");
        }

        AttendanceAnomaly anomaly =
                requireAnomaly(anomalyId);

        ensureCanReviewAnomaly(anomaly);

        boolean updated =
                anomalyRepository.updateEmployeeNotified(
                        anomalyId,
                        enabled
                );

        if (updated) {
            auditService.record(
                    enabled
                            ? "ATTENDANCE_ANOMALY_EMPLOYEE_NOTIFICATION_ENABLED"
                            : "ATTENDANCE_ANOMALY_EMPLOYEE_NOTIFICATION_DISABLED",
                    "ATTENDANCE_ANOMALY",
                    anomalyId,
                    enabled
                            ? "Employee notification enabled"
                            : "Employee notification disabled",
                    anomaly.getBranchId()
            );

            if (enabled) {
                notifyEmployee(anomaly, anomaly.getType(), anomaly.getMessage());
            }
        }

        return updated;
    }

    public boolean updateAttendanceFromReview(
            Long anomalyId,
            AttendanceSession updatedSession
    ) {

        if (!canAdminReview()) {
            denyReviewAccess(anomalyId, "Attendance edit denied");
        }

        if (updatedSession == null || updatedSession.getId() == null) {
            throw new RuntimeException("Attendance session is required");
        }

        AttendanceAnomaly anomaly =
                requireAnomaly(anomalyId);

        ensureCanReviewAnomaly(anomaly);

        AttendanceSession currentSession =
                attendanceRepository.findSessionById(updatedSession.getId());

        if (currentSession == null) {
            throw new RuntimeException("Attendance session not found");
        }

        if (updatedSession.getEmployeeId() != null) {
            currentSession.setEmployeeId(updatedSession.getEmployeeId());
        }

        if (updatedSession.getBranchId() != null) {
            currentSession.setBranchId(updatedSession.getBranchId());
        }

        currentSession.setCheckInTime(
                TimeFormatUtil.truncateToSeconds(
                        updatedSession.getCheckInTime()
                )
        );
        currentSession.setCheckOutTime(
                TimeFormatUtil.truncateToSeconds(
                        updatedSession.getCheckOutTime()
                )
        );

        if (updatedSession.getWorkedHours() != null) {
            currentSession.setWorkedHours(updatedSession.getWorkedHours());
        } else if (currentSession.getCheckInTime() != null
                && currentSession.getCheckOutTime() != null) {
            currentSession.setWorkedHours(
                    calculateWorkedHours(
                            currentSession.getCheckInTime(),
                            currentSession.getCheckOutTime()
                    )
            );
        }

        boolean updated =
                attendanceRepository.updateSession(currentSession);

        if (updated) {
            auditService.record(
                    "ATTENDANCE_EDITED_FROM_ANOMALY",
                    "ATTENDANCE_SESSION",
                    currentSession.getId(),
                    "Attendance edited from anomaly review",
                    anomaly.getBranchId()
            );

            evaluateSavedSession(currentSession);
        }

        return updated;
    }

    public void evaluateSavedSession(
            AttendanceSession session
    ) {

        if (session == null || session.getId() == null) {
            return;
        }

        WorkShiftRule rule =
                resolveRule(session.getBranchId());

        if (rule == null) {
            return;
        }

        detectCheckInAnomalies(session, rule);
        detectCheckOutAnomalies(session, rule);
        detectWorkHoursAnomalies(session, rule);
    }

    public void scanOpenSessionsForMissingCheckout() {

        for (AttendanceSession session : attendanceRepository.findOpenSessions()) {
            if (session == null
                    || session.getId() == null
                    || session.getCheckInTime() == null
                    || session.getBranchId() == null) {
                continue;
            }

            WorkShiftRule rule =
                    resolveRule(session.getBranchId());

            if (rule == null) {
                continue;
            }

            if (anomalyRepository.findByAttendanceSessionIdAndType(
                    session.getId(),
                    AttendanceAnomalyType.MISSING_CHECK_OUT
            ) != null) {
                continue;
            }

            LocalDateTime now =
                    TimeFormatUtil.truncateToSeconds(
                            LocalDateTime.now()
                    );
            LocalTime currentTime = now.toLocalTime();
            int currentMinutes = toMinutes(currentTime);
            int endMinutes = toMinutes(rule.getEndTime())
                    + safeMinutes(rule.getLateToleranceMinutes());

            if (currentMinutes < endMinutes
                    && Duration.between(session.getCheckInTime(), now)
                    .toHours() < safeHours(rule.getMaxWorkHours())) {
                continue;
            }

            String message =
                    "Missing check-out detected for "
                            + branchName(session.getBranchId())
                            + " starting at "
                            + TimeFormatUtil.formatTime(
                                    session.getCheckInTime().toLocalTime()
                            );

            createAnomalyIfAbsent(
                    session,
                    rule,
                    AttendanceAnomalyType.MISSING_CHECK_OUT,
                    AttendanceAnomalySeverity.HIGH,
                    message,
                    true
            );
        }
    }

    private void detectCheckInAnomalies(
            AttendanceSession session,
            WorkShiftRule rule
    ) {

        LocalTime checkInTime =
                session.getCheckInTime() == null
                        ? null
                        : session.getCheckInTime().toLocalTime();

        if (checkInTime == null) {
            return;
        }

        int checkInMinutes = toMinutes(checkInTime);
        int startMinutes = toMinutes(rule.getStartTime());
        int earlyTolerance = safeMinutes(rule.getEarlyToleranceMinutes());
        int lateTolerance = safeMinutes(rule.getLateToleranceMinutes());

        if (checkInMinutes < startMinutes - earlyTolerance) {
            createAnomalyIfAbsent(
                    session,
                    rule,
                    AttendanceAnomalyType.CHECK_IN_TOO_EARLY,
                    severityFromGap(startMinutes - checkInMinutes),
                    "Check-in too early at "
                            + TimeFormatUtil.formatTime(checkInTime)
                            + " against shift "
                            + TimeFormatUtil.formatTime(rule.getStartTime()),
                    true
            );
        } else if (checkInMinutes > startMinutes + lateTolerance) {
            createAnomalyIfAbsent(
                    session,
                    rule,
                    AttendanceAnomalyType.CHECK_IN_TOO_LATE,
                    severityFromGap(checkInMinutes - startMinutes),
                    "Check-in too late at "
                            + TimeFormatUtil.formatTime(checkInTime)
                            + " against shift "
                            + TimeFormatUtil.formatTime(rule.getStartTime()),
                    true
            );
        }

        if (checkInMinutes < startMinutes - earlyTolerance
                || checkInMinutes > toMinutes(rule.getEndTime()) + lateTolerance) {
            createAnomalyIfAbsent(
                    session,
                    rule,
                    AttendanceAnomalyType.OUT_OF_SHIFT,
                    severityFromGap(Math.abs(checkInMinutes - startMinutes)),
                    "Attendance outside shift range at check-in: "
                            + TimeFormatUtil.formatTime(checkInTime),
                    true
            );
        }
    }

    private void detectCheckOutAnomalies(
            AttendanceSession session,
            WorkShiftRule rule
    ) {

        LocalTime checkOutTime =
                session.getCheckOutTime() == null
                        ? null
                        : session.getCheckOutTime().toLocalTime();

        if (checkOutTime == null) {
            return;
        }

        int checkOutMinutes = toMinutes(checkOutTime);
        int endMinutes = toMinutes(rule.getEndTime());
        int earlyTolerance = safeMinutes(rule.getEarlyToleranceMinutes());
        int lateTolerance = safeMinutes(rule.getLateToleranceMinutes());

        if (checkOutMinutes < endMinutes - earlyTolerance) {
            createAnomalyIfAbsent(
                    session,
                    rule,
                    AttendanceAnomalyType.CHECK_OUT_TOO_EARLY,
                    severityFromGap(endMinutes - checkOutMinutes),
                    "Check-out too early at "
                            + TimeFormatUtil.formatTime(checkOutTime)
                            + " against shift "
                            + TimeFormatUtil.formatTime(rule.getEndTime()),
                    true
            );
        } else if (checkOutMinutes > endMinutes + lateTolerance) {
            createAnomalyIfAbsent(
                    session,
                    rule,
                    AttendanceAnomalyType.CHECK_OUT_TOO_LATE,
                    severityFromGap(checkOutMinutes - endMinutes),
                    "Check-out too late at "
                            + TimeFormatUtil.formatTime(checkOutTime)
                            + " against shift "
                            + TimeFormatUtil.formatTime(rule.getEndTime()),
                    true
            );
        }

        if (checkOutMinutes < toMinutes(rule.getStartTime()) - earlyTolerance
                || checkOutMinutes > endMinutes + lateTolerance) {
            createAnomalyIfAbsent(
                    session,
                    rule,
                    AttendanceAnomalyType.OUT_OF_SHIFT,
                    severityFromGap(Math.abs(checkOutMinutes - endMinutes)),
                    "Attendance outside shift range at check-out: "
                            + TimeFormatUtil.formatTime(checkOutTime),
                    true
            );
        }
    }

    private void detectWorkHoursAnomalies(
            AttendanceSession session,
            WorkShiftRule rule
    ) {

        if (session.getWorkedHours() == null) {
            return;
        }

        BigDecimal workedHours =
                session.getWorkedHours().setScale(2, RoundingMode.HALF_UP);

        BigDecimal expectedHours =
                expectedShiftHours(rule);

        BigDecimal shortThreshold =
                expectedHours.subtract(
                        BigDecimal.valueOf(
                                minutesToHours(
                                        safeMinutes(rule.getEarlyToleranceMinutes())
                                                + safeMinutes(rule.getLateToleranceMinutes())
                                )
                        )
                );

        if (shortThreshold.compareTo(BigDecimal.ZERO) < 0) {
            shortThreshold = BigDecimal.ZERO;
        }

        if (workedHours.compareTo(shortThreshold) < 0) {
            createAnomalyIfAbsent(
                    session,
                    rule,
                    AttendanceAnomalyType.WORKED_TOO_SHORT,
                    severityFromDifference(
                            shortThreshold.subtract(workedHours).doubleValue()
                    ),
                    "Worked hours too short: "
                            + TimeFormatUtil.formatDurationHours(workedHours)
                            + " against expected "
                            + TimeFormatUtil.formatDurationHours(expectedHours),
                    true
            );
        }

        if (workedHours.compareTo(rule.getMaxWorkHours()) > 0) {
            createAnomalyIfAbsent(
                    session,
                    rule,
                    AttendanceAnomalyType.WORKED_TOO_LONG,
                    severityFromDifference(
                            workedHours.subtract(rule.getMaxWorkHours()).doubleValue()
                    ),
                    "Worked hours too long: "
                            + TimeFormatUtil.formatDurationHours(workedHours)
                            + " exceeds max "
                            + TimeFormatUtil.formatDurationHours(
                                    rule.getMaxWorkHours()
                            ),
                    true
            );
        }
    }

    private boolean createAnomalyIfAbsent(
            AttendanceSession session,
            WorkShiftRule rule,
            AttendanceAnomalyType type,
            AttendanceAnomalySeverity severity,
            String message,
            boolean notifyEmployeeIfEnabled
    ) {

        if (session == null || session.getId() == null || type == null) {
            return false;
        }

        AttendanceAnomaly existing =
                anomalyRepository.findByAttendanceSessionIdAndType(
                        session.getId(),
                        type
                );

        if (existing != null) {
            return false;
        }

        AttendanceAnomaly anomaly =
                new AttendanceAnomaly();

        anomaly.setAttendanceSessionId(session.getId());
        anomaly.setEmployeeId(session.getEmployeeId());
        anomaly.setBranchId(session.getBranchId());
        anomaly.setType(type);
        anomaly.setSeverity(
                severity == null ? AttendanceAnomalySeverity.LOW : severity
        );
        anomaly.setMessage(message);
        anomaly.setStatus(AttendanceAnomalyStatus.OPEN);
        anomaly.setEmployeeNotified(notifyEmployeeIfEnabled);
        anomaly.setManagerReported(false);

        boolean saved =
                anomalyRepository.save(anomaly);

        if (!saved) {
            return false;
        }

        auditService.record(
                "ATTENDANCE_ANOMALY_CREATED",
                "ATTENDANCE_ANOMALY",
                anomaly.getId(),
                message,
                session.getBranchId()
        );

        notifyReviewStakeholders(
                anomaly,
                notificationTitleFor(type),
                message
        );

        return true;
    }

    private void notifyReviewStakeholders(
            AttendanceAnomaly anomaly,
            String title,
            String content
    ) {

        if (anomaly == null) {
            return;
        }

        Set<Long> notifiedUserIds =
                new java.util.LinkedHashSet<>();

        for (User user : findAdministrativeRecipients()) {
            if (user == null || user.getId() == null) {
                continue;
            }

            if (notifiedUserIds.add(user.getId())) {
                notificationService.notifyUser(
                        user.getId(),
                        title,
                        content,
                        NotificationType.ATTENDANCE_ANOMALY
                );
            }
        }

        for (User user : findManagerRecipientsForBranch(anomaly.getBranchId())) {
            if (user == null || user.getId() == null) {
                continue;
            }

            if (notifiedUserIds.add(user.getId())) {
                notificationService.notifyUser(
                        user.getId(),
                        title,
                        content,
                        NotificationType.ATTENDANCE_ANOMALY
                );
            }
        }

        if (anomaly.isEmployeeNotified()) {
            notifyEmployee(anomaly, anomaly.getType(), content);
        }
    }

    private void notifyEmployee(
            AttendanceAnomaly anomaly,
            AttendanceAnomalyType type,
            String content
    ) {

        if (anomaly == null || anomaly.getEmployeeId() == null) {
            return;
        }

        Employee employee =
                employeeRepository.findById(anomaly.getEmployeeId());

        if (employee == null || employee.getUserId() == null) {
            return;
        }

        User user =
                userRepository.findById(employee.getUserId())
                        .orElse(null);

        if (user == null || !user.isActive()) {
            return;
        }

        notificationService.notifyUser(
                user.getId(),
                titleForEmployee(type),
                content,
                NotificationType.ATTENDANCE_ANOMALY
        );

        anomalyRepository.updateEmployeeNotified(
                anomaly.getId(),
                true
        );
    }

    private List<User> findAdministrativeRecipients() {

        return userRepository.findAll()
                .stream()
                .filter(User::isActive)
                .filter(user ->
                        user.getRole() == RoleType.DEVELOPER
                                || user.getRole() == RoleType.OWNER
                )
                .toList();
    }

    private List<User> findManagerRecipientsForBranch(
            Long branchId
    ) {

        if (branchId == null) {
            return List.of();
        }

        Map<Long, Employee> employeesById =
                employeeRepository.findAll()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Employee::getId,
                                        Function.identity(),
                                        (left, right) -> left
                                )
                        );

        Map<Long, User> usersById =
                userRepository.findAll()
                        .stream()
                        .filter(User::isActive)
                        .collect(
                                Collectors.toMap(
                                        User::getId,
                                        Function.identity(),
                                        (left, right) -> left
                                )
                        );

        return branchRepository.findAllAssignments()
                .stream()
                .filter(EmployeeBranchAssignment::isActive)
                .filter(assignment -> branchId.equals(assignment.getBranchId()))
                .map(EmployeeBranchAssignment::getEmployeeId)
                .distinct()
                .map(employeesById::get)
                .filter(Objects::nonNull)
                .map(Employee::getUserId)
                .filter(Objects::nonNull)
                .map(usersById::get)
                .filter(Objects::nonNull)
                .filter(user -> user.getRole() == RoleType.MANAGER)
                .toList();
    }

    private void ensureCanReviewAnomaly(
            AttendanceAnomaly anomaly
    ) {

        if (anomaly == null) {
            return;
        }

        ensureCanReviewBranch(anomaly.getBranchId());
    }

    private void ensureCanReviewBranch(
            Long branchId
    ) {

        if (PermissionGuard.isDeveloper()
                || PermissionGuard.isOwner()) {
            return;
        }

        if (PermissionGuard.isManager()) {
            List<Long> branchIds = getManagerBranchIds();
            if (branchId == null || branchIds.contains(branchId)) {
                return;
            }
            denyReviewAccess(branchId, "Attendance anomaly access denied");
        }

        denyReviewAccess(branchId, "Attendance anomaly access denied");
    }

    private void denyReviewAccess(
            Long entityId,
            String message
    ) {

        auditService.recordPermissionDenied(
                AuditService.ACTION_PERMISSION_DENIED,
                "ATTENDANCE_ANOMALY",
                entityId,
                message,
                null
        );
        throw new RuntimeException(message);
    }

    private boolean canAdminReview() {

        return PermissionGuard.isDeveloper()
                || PermissionGuard.isOwner();
    }

    private boolean canManageAnomalyReview() {

        return canAdminReview() || PermissionGuard.isManager();
    }

    private boolean canManagerReport() {

        return PermissionGuard.isManager();
    }

    private AttendanceAnomaly requireAnomaly(
            Long anomalyId
    ) {

        AttendanceAnomaly anomaly =
                anomalyRepository.findById(anomalyId);

        if (anomaly == null) {
            throw new RuntimeException("Attendance anomaly not found");
        }

        return anomaly;
    }

    private AttendanceAnomalyFilter copyFilter(
            AttendanceAnomalyFilter filter
    ) {

        AttendanceAnomalyFilter copy =
                new AttendanceAnomalyFilter();

        if (filter == null) {
            return copy;
        }

        copy.setStatus(filter.getStatus());
        copy.setType(filter.getType());
        copy.setSeverity(filter.getSeverity());
        copy.setEmployeeId(filter.getEmployeeId());
        copy.setBranchId(filter.getBranchId());
        copy.setBranchIds(filter.getBranchIds());
        copy.setFromDate(filter.getFromDate());
        copy.setToDate(filter.getToDate());
        return copy;
    }

    private List<Long> getManagerBranchIds() {

        Employee currentEmployee =
                getCurrentEmployee();

        if (currentEmployee == null) {
            return List.of();
        }

        return branchRepository.findActiveBranchesForEmployeeId(
                        currentEmployee.getId()
                )
                .stream()
                .map(Branch::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private WorkShiftRule resolveRule(
            Long branchId
    ) {

        if (branchId == null) {
            return null;
        }

        WorkShiftRule rule =
                workShiftRuleRepository.findActiveByBranchId(branchId);

        if (rule != null) {
            return rule;
        }

        WorkShiftRule fallback =
                new WorkShiftRule();
        fallback.setBranchId(branchId);
        fallback.setShiftName("Default shift");
        fallback.setStartTime(DEFAULT_SHIFT_START);
        fallback.setEndTime(DEFAULT_SHIFT_END);
        fallback.setEarlyToleranceMinutes(DEFAULT_EARLY_TOLERANCE_MINUTES);
        fallback.setLateToleranceMinutes(DEFAULT_LATE_TOLERANCE_MINUTES);
        fallback.setMaxWorkHours(DEFAULT_MAX_WORK_HOURS);
        fallback.setActive(true);
        return fallback;
    }

    private User getCurrentUser() {

        return AppSession.getCurrentUser();
    }

    private RoleType getCurrentRole() {

        User currentUser =
                getCurrentUser();

        return currentUser == null
                ? null
                : currentUser.getRole();
    }

    private Employee getCurrentEmployee() {

        User currentUser =
                getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return employeeRepository.findByUserId(
                currentUser.getId()
        );
    }

    private Long getCurrentUserId() {

        User currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.getId();
    }

    private int toMinutes(
            LocalTime time
    ) {

        if (time == null) {
            return 0;
        }

        return time.getHour() * 60 + time.getMinute();
    }

    private int safeMinutes(
            Integer minutes
    ) {

        return minutes == null ? 0 : minutes;
    }

    private int safeMinutes(
            BigDecimal hours
    ) {

        return hours == null
                ? 0
                : hours.multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private double safeHours(
            BigDecimal hours
    ) {

        return hours == null ? 0D : hours.doubleValue();
    }

    private BigDecimal expectedShiftHours(
            WorkShiftRule rule
    ) {

        if (rule == null || rule.getStartTime() == null
                || rule.getEndTime() == null) {
            return DEFAULT_MAX_WORK_HOURS;
        }

        int startMinutes = toMinutes(rule.getStartTime());
        int endMinutes = toMinutes(rule.getEndTime());

        if (endMinutes < startMinutes) {
            endMinutes += 24 * 60;
        }

        return BigDecimal.valueOf(
                minutesToHours(endMinutes - startMinutes)
        ).setScale(2, RoundingMode.HALF_UP);
    }

    private double minutesToHours(
            int minutes
    ) {

        return minutes / 60.0;
    }

    private AttendanceAnomalySeverity severityFromGap(
            long minutes
    ) {

        if (minutes >= 120) {
            return AttendanceAnomalySeverity.HIGH;
        }

        if (minutes >= 30) {
            return AttendanceAnomalySeverity.MEDIUM;
        }

        return AttendanceAnomalySeverity.LOW;
    }

    private AttendanceAnomalySeverity severityFromDifference(
            double hours
    ) {

        if (hours >= 2.0) {
            return AttendanceAnomalySeverity.HIGH;
        }

        if (hours >= 0.5) {
            return AttendanceAnomalySeverity.MEDIUM;
        }

        return AttendanceAnomalySeverity.LOW;
    }

    private String branchName(
            Long branchId
    ) {

        if (branchId == null) {
            return "unknown branch";
        }

        Branch branch =
                branchRepository.findBranchById(branchId);

        if (branch == null || branch.getName() == null) {
            return "branch #" + branchId;
        }

        return branch.getName();
    }

    private String notificationTitleFor(
            AttendanceAnomalyType type
    ) {

        if (type == null) {
            return "Abnormal attendance detected";
        }

        return switch (type) {
            case CHECK_IN_TOO_EARLY -> "Early check-in detected";
            case CHECK_IN_TOO_LATE -> "Late check-in detected";
            case CHECK_OUT_TOO_EARLY -> "Early check-out detected";
            case CHECK_OUT_TOO_LATE -> "Late check-out detected";
            case WORKED_TOO_SHORT -> "Short work shift detected";
            case WORKED_TOO_LONG -> "Long work shift detected";
            case MISSING_CHECK_OUT -> "Missing check-out detected";
            case OUT_OF_SHIFT -> "Out-of-shift attendance detected";
        };
    }

    private String titleForEmployee(
            AttendanceAnomalyType type
    ) {

        return notificationTitleFor(type);
    }

    private String clean(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private BigDecimal calculateWorkedHours(
            LocalDateTime checkInTime,
            LocalDateTime checkOutTime
    ) {

        if (checkInTime == null || checkOutTime == null) {
            return BigDecimal.ZERO;
        }

        double hours =
                Duration.between(checkInTime, checkOutTime).toMinutes() / 60.0;

        return BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);
    }
}
