package com.storemanager.domain.audit.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.audit.model.AuditLogFilter;
import com.storemanager.domain.audit.model.AuditLogViewDto;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.audit.view.AuditLogController;

import java.util.List;

public class AuditLogPresenter extends BaseModulePresenter {

    private final AuditLogController view;

    private final AuditService auditService =
            new AuditService();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public AuditLogPresenter(
            AuditLogController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        loadAuditLogs(new AuditLogFilter());
    }

    public void loadAuditLogs(
            AuditLogFilter filter
    ) {

        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading audit logs...");

        AsyncTaskRunner.run(
                () -> auditService.findAuditLogs(filter),
                logs -> {
                    view.setAuditLogs(logs);
                    loadingState = LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot load audit logs");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void refresh() {

        loadAuditLogs(view.buildFilter());
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }
}
