package com.storemanager.domain.dashboard.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.dashboard.model.DashboardAnalyticsSnapshot;
import com.storemanager.domain.dashboard.service.DashboardAnalyticsService;
import com.storemanager.domain.dashboard.view.DashboardHomeController;

public class DashboardAnalyticsPresenter extends BaseModulePresenter {

    private final DashboardHomeController view;

    private final DashboardAnalyticsService analyticsService =
            new DashboardAnalyticsService();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public DashboardAnalyticsPresenter(
            DashboardHomeController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        loadDashboard();
    }

    public void loadDashboard() {

        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading dashboard analytics...");

        AsyncTaskRunner.run(
                analyticsService::loadDashboardAnalytics,
                snapshot -> {
                    view.renderSnapshot(snapshot);
                    loadingState = LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot load dashboard analytics");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }
}
