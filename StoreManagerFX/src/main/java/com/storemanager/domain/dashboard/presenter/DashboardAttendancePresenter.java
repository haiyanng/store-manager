package com.storemanager.domain.dashboard.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.attendance.service.AttendanceService;
import com.storemanager.domain.dashboard.view.DashboardHomeController;

public class DashboardAttendancePresenter extends BaseModulePresenter {

    private final DashboardHomeController view;

    private final AttendanceService attendanceService =
            new AttendanceService();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public DashboardAttendancePresenter(
            DashboardHomeController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        loadQuickAttendance();
    }

    public void loadQuickAttendance() {

        loadingState = LoadingState.LOADING;
        view.setQuickAttendanceBusy(true);

        AsyncTaskRunner.run(
                attendanceService::getCurrentRuntimeStatus,
                status -> {
                    view.renderQuickAttendance(status);
                    loadingState = LoadingState.SUCCESS;
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.showError(throwable.getMessage());
                },
                () -> view.setQuickAttendanceBusy(false)
        );
    }

    public void checkIn() {

        runSelfAction(
                "Checking in...",
                attendanceService::checkInSelf
        );
    }

    public void checkOut() {

        runSelfAction(
                "Checking out...",
                attendanceService::checkOutSelf
        );
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private void runSelfAction(
            String loadingMessage,
            SelfAction action
    ) {

        loadingState = LoadingState.LOADING;
        view.setQuickAttendanceBusy(true);
        view.setQuickAttendanceStatus(loadingMessage);

        AsyncTaskRunner.run(
                action::run,
                success -> {
                    if (!success) {
                        loadingState = LoadingState.ERROR;
                        view.showError("Attendance action failed");
                        view.setQuickAttendanceBusy(false);
                        return;
                    }

                    view.onQuickAttendanceUpdated();
                    loadingState = LoadingState.SUCCESS;
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.showError(throwable.getMessage());
                    view.setQuickAttendanceBusy(false);
                },
                null
        );
    }

    private interface SelfAction {

        boolean run();
    }
}
