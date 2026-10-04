package com.storemanager.domain.audit.view;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashSet;
import java.util.Set;

public final class AuditChangeFormatter {
    private AuditChangeFormatter() { }

    public static String summary(String details) {
        if (details == null || details.isBlank()) return "No additional details";
        try {
            JsonObject data = JsonParser.parseString(details).getAsJsonObject();
            if (!data.has("before") || !data.has("after")) return details;
            JsonObject before = object(data.get("before"));
            JsonObject after = object(data.get("after"));
            Set<String> keys = new LinkedHashSet<>(before.keySet());
            keys.addAll(after.keySet());
            StringBuilder result = new StringBuilder();
            for (String key : keys) {
                JsonElement oldValue = before.get(key);
                JsonElement newValue = after.get(key);
                if (java.util.Objects.equals(oldValue, newValue)) continue;
                if (!result.isEmpty()) result.append("\n");
                result.append(key.replace('_', ' ')).append(": ")
                        .append(display(oldValue)).append(" → ").append(display(newValue));
            }
            JsonObject attempted = object(data.get("attempted"));
            if (attempted.has("password_changed") && attempted.get("password_changed").getAsBoolean()) {
                if (!result.isEmpty()) result.append("\n");
                result.append("Password change requested");
            }
            return result.isEmpty() ? "No saved field changes; see event details." : result.toString();
        } catch (RuntimeException e) {
            return details; // Legacy free-text audit entries remain readable.
        }
    }

    public static String pretty(String details) {
        if (details == null || details.isBlank()) return "No additional details";
        try {
            return new GsonBuilder().setPrettyPrinting().serializeNulls().create()
                    .toJson(JsonParser.parseString(details));
        } catch (RuntimeException e) {
            return details;
        }
    }

    private static JsonObject object(JsonElement value) {
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    private static String display(JsonElement value) {
        if (value == null || value.isJsonNull()) return "(none)";
        return value.isJsonPrimitive() ? value.getAsString() : value.toString();
    }
}
