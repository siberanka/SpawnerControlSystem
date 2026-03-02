package com.siberanka.spawnercontrolsystem.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.List;

public class ConditionEngine {

    // Evaluates a list of groups (OR logic between groups)
    public static boolean evaluateAnyGroup(Player player, List<List<String>> conditionGroups) {
        if (conditionGroups == null || conditionGroups.isEmpty())
            return true;

        for (List<String> group : conditionGroups) {
            if (evaluateGroup(player, group)) {
                return true; // At least one group passed!
            }
        }
        return false;
    }

    // Evaluates all conditions in a single group (AND logic within a group)
    private static boolean evaluateGroup(Player player, List<String> conditions) {
        for (String condition : conditions) {
            if (!evaluate(player, condition)) {
                return false;
            }
        }
        return true;
    }

    public static boolean evaluate(Player player, String conditionStr) {
        // Evaluate operators sorted by length to prevent partial matches (e.g. >=
        // matching >)
        String[] operators = { ">=", "<=", "!=", "=", ">", "<" };
        String operator = null;
        int opIndex = -1;

        for (String op : operators) {
            int index = conditionStr.indexOf(op);
            if (index != -1) {
                operator = op;
                opIndex = index;
                break;
            }
        }

        // If no valid operator is found, the condition format is invalid; default to
        // false to prevent exploits
        if (operator == null)
            return false;

        String leftRaw = conditionStr.substring(0, opIndex).trim();
        String rightRaw = conditionStr.substring(opIndex + operator.length()).trim();

        // Safe placeholder parsing using PlaceholderAPI
        String left = PlaceholderAPI.setPlaceholders(player, leftRaw);
        String right = PlaceholderAPI.setPlaceholders(player, rightRaw);

        switch (operator) {
            case ">=":
            case "<=":
            case ">":
            case "<":
                try {
                    double l = Double.parseDouble(left);
                    double r = Double.parseDouble(right);
                    if (operator.equals(">="))
                        return l >= r;
                    if (operator.equals("<="))
                        return l <= r;
                    if (operator.equals(">"))
                        return l > r;
                    if (operator.equals("<"))
                        return l < r;
                } catch (NumberFormatException e) {
                    // String comparison not supported for inequalities
                    return false;
                }
                break;
            case "=":
                try {
                    double l = Double.parseDouble(left);
                    double r = Double.parseDouble(right);
                    return l == r;
                } catch (NumberFormatException e) {
                    return left.equalsIgnoreCase(right);
                }
            case "!=":
                try {
                    double l = Double.parseDouble(left);
                    double r = Double.parseDouble(right);
                    return l != r;
                } catch (NumberFormatException e) {
                    return !left.equalsIgnoreCase(right);
                }
        }
        return false;
    }
}
