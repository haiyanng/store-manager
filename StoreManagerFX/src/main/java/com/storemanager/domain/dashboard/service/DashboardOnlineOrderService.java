package com.storemanager.domain.dashboard.service;

import com.storemanager.domain.dashboard.model.DashboardOnlineOrderSnapshot;
import com.storemanager.domain.dashboard.repository.DashboardOnlineOrderRepository;

public class DashboardOnlineOrderService {

    private final DashboardOnlineOrderRepository repository =
            new DashboardOnlineOrderRepository();

    public DashboardOnlineOrderSnapshot loadDashboard() {
        return repository.loadSnapshot(10);
    }
}
