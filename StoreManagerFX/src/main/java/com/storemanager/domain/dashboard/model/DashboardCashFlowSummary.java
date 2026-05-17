package com.storemanager.domain.dashboard.model;

import java.math.BigDecimal;

public class DashboardCashFlowSummary {

    private String label;

    private BigDecimal revenueTotal;

    private BigDecimal importCost;

    private BigDecimal productBusinessCashFlow;

    private BigDecimal payrollCashFlow;

    private BigDecimal finalCashFlow;

    public DashboardCashFlowSummary() {
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getRevenueTotal() {
        return revenueTotal;
    }

    public void setRevenueTotal(BigDecimal revenueTotal) {
        this.revenueTotal = revenueTotal;
    }

    public BigDecimal getImportCost() {
        return importCost;
    }

    public void setImportCost(BigDecimal importCost) {
        this.importCost = importCost;
    }

    public BigDecimal getProductBusinessCashFlow() {
        return productBusinessCashFlow;
    }

    public void setProductBusinessCashFlow(BigDecimal productBusinessCashFlow) {
        this.productBusinessCashFlow = productBusinessCashFlow;
    }

    public BigDecimal getPayrollCashFlow() {
        return payrollCashFlow;
    }

    public void setPayrollCashFlow(BigDecimal payrollCashFlow) {
        this.payrollCashFlow = payrollCashFlow;
    }

    public BigDecimal getFinalCashFlow() {
        return finalCashFlow;
    }

    public void setFinalCashFlow(BigDecimal finalCashFlow) {
        this.finalCashFlow = finalCashFlow;
    }
}
