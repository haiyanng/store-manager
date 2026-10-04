package com.storemanager.domain.sale.service;

import com.storemanager.domain.sale.model.SaleOrder;
import com.storemanager.domain.sale.model.SaleOrderItemDetail;
import com.storemanager.core.util.MoneyFormatUtil;
import com.storemanager.core.util.TimeFormatUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.io.IOException;

public class InvoiceService {
    public void export(SaleOrder order, Path destination) throws IOException {
        var sales = new SaleService();
        var stored = sales.findOrderById(order.getId());
        var details = sales.findOrderItemDetails(stored);
        Files.writeString(destination, render(stored, details), StandardCharsets.UTF_8);
    }

    public static String render(SaleOrder order, List<SaleOrderItemDetail> details) {
        StringBuilder html = new StringBuilder("""
                <!doctype html><html lang="en"><meta charset="UTF-8">
                <title>Sale invoice</title><style>
                body{font:16px Arial,sans-serif;max-width:900px;margin:40px auto;padding:20px;color:#17212b}
                table{width:100%;border-collapse:collapse}th,td{padding:12px;border-bottom:1px solid #ddd;text-align:right}
                th:first-child,td:first-child{text-align:left}.totals{text-align:right;margin-top:24px}
                @media print{body{margin:0}button{display:none}}
                </style><body><h1>Store Manager</h1>
                """);
        html.append("<h2>Invoice #").append(order.getId()).append("</h2><p>Date: ")
                .append(escape(TimeFormatUtil.formatDateTime(order.getCreatedAt())))
                .append("</p><p>Cashier ID: ").append(order.getCreatedByUserId())
                .append("</p><table><thead><tr><th>Product / SKU</th><th>Quantity</th><th>Unit price</th><th>Subtotal</th></tr></thead><tbody>");
        for (var item : details) html.append("<tr><td>").append(escape(item.getProductName()))
                .append(" / ").append(escape(item.getSku())).append("</td><td>").append(item.getQuantity())
                .append("</td><td>").append(escape(MoneyFormatUtil.format(item.getUnitPrice())))
                .append("</td><td>").append(escape(MoneyFormatUtil.format(item.getSubtotal()))).append("</td></tr>");
        html.append("</tbody></table><div class='totals'><p><strong>Total: ")
                .append(escape(MoneyFormatUtil.format(order.getTotalAmount()))).append("</strong></p>");
        if (order.getAmountReceived() != null) html.append("<p>Amount received: ")
                .append(escape(MoneyFormatUtil.format(order.getAmountReceived()))).append("</p><p>Change: ")
                .append(escape(MoneyFormatUtil.format(order.getChangeAmount()))).append("</p>");
        return html.append("</div><p>Thank you for your purchase.</p><button onclick='window.print()'>Print / Save as PDF</button></body></html>").toString();
    }

    private static String escape(String value) {
        return value == null ? "-" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
