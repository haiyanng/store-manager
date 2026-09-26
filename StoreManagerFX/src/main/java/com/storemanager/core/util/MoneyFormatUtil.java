package com.storemanager.core.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyFormatUtil {
    private MoneyFormatUtil() { }

    public static String format(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(amount == null ? BigDecimal.ZERO : amount) + " VND";
    }
}
