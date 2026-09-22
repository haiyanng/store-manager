package com.customershopfx.shop.presenter;

import com.customershopfx.app.SceneManager;
import com.customershopfx.app.SessionManager;
import com.customershopfx.shop.view.ShopController;
import com.customershopfx.shop.viewmodel.ShopViewModel;

public class ShopPresenter {
    private final ShopController view;
    private final ShopViewModel viewModel;

    public ShopPresenter(ShopController view, ShopViewModel viewModel) {
        this.view = view;
        this.viewModel = viewModel;
    }

    public void initialize() {
        view.showCustomer(SessionManager.customer());
        viewModel.setSelectedTab(0);
    }

    public void logout() {
        SessionManager.clear();
        SceneManager.showLogin();
    }

    public ShopViewModel viewModel() {
        return viewModel;
    }
}
