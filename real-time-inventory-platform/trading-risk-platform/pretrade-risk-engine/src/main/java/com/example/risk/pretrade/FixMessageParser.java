package com.example.risk.pretrade;

import com.example.risk.pretrade.Models.OrderRequest;
import com.example.risk.pretrade.Models.Side;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FixMessageParser {
    public OrderRequest parse(String rawMessage) {
        Map<String, String> tags = parseTags(rawMessage);
        String messageType = require(tags, "35", "message type");
        if (!"D".equals(messageType)) {
            throw new IllegalArgumentException("Only FIX NewOrderSingle messages are supported. Expected 35=D.");
        }

        return new OrderRequest(
                require(tags, "11", "client order id"),
                require(tags, "1", "account"),
                require(tags, "55", "symbol"),
                Side.fromFix(require(tags, "54", "side")),
                parseLong(require(tags, "38", "quantity"), "quantity"),
                parseDecimal(require(tags, "44", "price"), "price"),
                false);
    }

    public Map<String, String> parseTags(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("FIX message is required");
        }

        String normalized = rawMessage
                .replace('\u0001', '|')
                .replace('\n', '|')
                .replace('\r', '|')
                .trim();

        Map<String, String> tags = Arrays.stream(normalized.split("\\|"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2 && !parts[0].isBlank())
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> parts[1],
                        (left, right) -> right,
                        LinkedHashMap::new));

        if (tags.isEmpty()) {
            throw new IllegalArgumentException("No FIX tags were found");
        }
        return tags;
    }

    private static String require(Map<String, String> tags, String tag, String label) {
        String value = tags.get(tag);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing FIX " + label + " tag " + tag);
        }
        return value.trim();
    }

    private static long parseLong(String value, String label) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid FIX " + label + ": " + value);
        }
    }

    private static BigDecimal parseDecimal(String value, String label) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid FIX " + label + ": " + value);
        }
    }
}
