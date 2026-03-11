package com.ruben.bblib.api.molang;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MolangParser {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*");
    private static final Logger LOGGER = Logger.getLogger(MolangParser.class.getName());

    private MolangParser() {
    }

    public static MolangValue parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return ctx -> 0;
        }

        expression = expression.trim().toLowerCase();

        try {
            return parseExpression(new ParseState(expression));
        } catch (Exception e) {
            LOGGER.warning("Failed to parse Molang expression '" + expression + "': " + e.getMessage());
            return ctx -> 0;
        }
    }

    public static boolean isMolangExpression(String value) {
        if (value == null) return false;
        String trimmed = value.trim().toLowerCase();
        return trimmed.contains("math.") ||
                trimmed.contains("query.") ||
                trimmed.contains("q.") ||
                trimmed.contains("+") ||
                trimmed.contains("-") && !trimmed.matches("^-?\\d+(\\.\\d+)?$") ||
                trimmed.contains("*") ||
                trimmed.contains("/");
    }

    private static MolangValue parseExpression(ParseState state) {
        return parseAddSub(state);
    }

    private static MolangValue parseAddSub(ParseState state) {
        MolangValue left = parseMulDiv(state);
        state.skipWhitespace();

        while (state.hasMore()) {
            char c = state.peek();
            if (c == '+') {
                state.advance();
                MolangValue right = parseMulDiv(state);
                MolangValue finalLeft = left;
                left = ctx -> finalLeft.evaluate(ctx) + right.evaluate(ctx);
            } else if (c == '-') {
                state.advance();
                MolangValue right = parseMulDiv(state);
                MolangValue finalLeft = left;
                left = ctx -> finalLeft.evaluate(ctx) - right.evaluate(ctx);
            } else {
                break;
            }
            state.skipWhitespace();
        }

        return left;
    }

    private static MolangValue parseMulDiv(ParseState state) {
        MolangValue left = parseUnary(state);
        state.skipWhitespace();

        while (state.hasMore()) {
            char c = state.peek();
            if (c == '*') {
                state.advance();
                MolangValue right = parseUnary(state);
                MolangValue finalLeft = left;
                left = ctx -> finalLeft.evaluate(ctx) * right.evaluate(ctx);
            } else if (c == '/') {
                state.advance();
                MolangValue right = parseUnary(state);
                MolangValue finalLeft = left;
                left = ctx -> {
                    double divisor = right.evaluate(ctx);
                    return divisor == 0 ? 0 : finalLeft.evaluate(ctx) / divisor;
                };
            } else if (c == '%') {
                state.advance();
                MolangValue right = parseUnary(state);
                MolangValue finalLeft = left;
                left = ctx -> {
                    double divisor = right.evaluate(ctx);
                    return divisor == 0 ? 0 : finalLeft.evaluate(ctx) % divisor;
                };
            } else {
                break;
            }
            state.skipWhitespace();
        }

        return left;
    }

    private static MolangValue parseUnary(ParseState state) {
        state.skipWhitespace();

        if (state.hasMore() && state.peek() == '-') {
            state.advance();
            MolangValue value = parsePrimary(state);
            return ctx -> -value.evaluate(ctx);
        }

        return parsePrimary(state);
    }

    private static MolangValue parsePrimary(ParseState state) {
        state.skipWhitespace();

        if (!state.hasMore()) {
            return ctx -> 0;
        }

        char c = state.peek();

        if (c == '(') {
            state.advance();
            MolangValue inner = parseExpression(state);
            state.skipWhitespace();
            if (state.hasMore() && state.peek() == ')') {
                state.advance();
            }
            return inner;
        }

        if (Character.isDigit(c) || c == '.') {
            return parseNumber(state);
        }

        if (Character.isLetter(c) || c == '_') {
            return parseIdentifier(state);
        }

        return ctx -> 0;
    }

    private static MolangValue parseNumber(ParseState state) {
        Matcher matcher = NUMBER_PATTERN.matcher(state.remaining());
        if (matcher.find()) {
            String numStr = matcher.group();
            state.advance(numStr.length());
            double value = Double.parseDouble(numStr);
            return ctx -> value;
        }
        return ctx -> 0;
    }

    private static MolangValue parseIdentifier(ParseState state) {
        Matcher matcher = IDENTIFIER_PATTERN.matcher(state.remaining());
        if (!matcher.find()) {
            return ctx -> 0;
        }

        String identifier = matcher.group();
        state.advance(identifier.length());
        state.skipWhitespace();

        if (state.hasMore() && state.peek() == '(') {
            return parseFunction(identifier, state);
        }

        return parseVariable(identifier);
    }

    private static MolangValue parseFunction(String name, ParseState state) {
        state.advance();
        List<MolangValue> args = new ArrayList<>();
        state.skipWhitespace();

        if (state.hasMore() && state.peek() != ')') {
            args.add(parseExpression(state));
            state.skipWhitespace();

            while (state.hasMore() && state.peek() == ',') {
                state.advance();
                args.add(parseExpression(state));
                state.skipWhitespace();
            }
        }

        if (state.hasMore() && state.peek() == ')') {
            state.advance();
        }

        return createFunction(name, args);
    }

    private static MolangValue createFunction(String name, List<MolangValue> args) {
        return switch (name) {
            case "math.sin" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.sin(Math.toRadians(arg.evaluate(ctx)));
            }
            case "math.cos" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.cos(Math.toRadians(arg.evaluate(ctx)));
            }
            case "math.tan" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.tan(Math.toRadians(arg.evaluate(ctx)));
            }
            case "math.asin" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.toDegrees(Math.asin(arg.evaluate(ctx)));
            }
            case "math.acos" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.toDegrees(Math.acos(arg.evaluate(ctx)));
            }
            case "math.atan" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.toDegrees(Math.atan(arg.evaluate(ctx)));
            }
            case "math.atan2" -> {
                MolangValue y = !args.isEmpty() ? args.get(0) : ctx -> 0;
                MolangValue x = args.size() > 1 ? args.get(1) : ctx -> 0;
                yield ctx -> Math.toDegrees(Math.atan2(y.evaluate(ctx), x.evaluate(ctx)));
            }
            case "math.abs" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.abs(arg.evaluate(ctx));
            }
            case "math.floor" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.floor(arg.evaluate(ctx));
            }
            case "math.ceil" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.ceil(arg.evaluate(ctx));
            }
            case "math.round" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.round(arg.evaluate(ctx));
            }
            case "math.trunc" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> (int) arg.evaluate(ctx);
            }
            case "math.sqrt" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.sqrt(arg.evaluate(ctx));
            }
            case "math.pow" -> {
                MolangValue base = !args.isEmpty() ? args.get(0) : ctx -> 0;
                MolangValue exp = args.size() > 1 ? args.get(1) : ctx -> 0;
                yield ctx -> Math.pow(base.evaluate(ctx), exp.evaluate(ctx));
            }
            case "math.exp" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.exp(arg.evaluate(ctx));
            }
            case "math.ln" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.log(arg.evaluate(ctx));
            }
            case "math.log" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.log10(arg.evaluate(ctx));
            }
            case "math.min" -> {
                MolangValue a = !args.isEmpty() ? args.get(0) : ctx -> 0;
                MolangValue b = args.size() > 1 ? args.get(1) : ctx -> 0;
                yield ctx -> Math.min(a.evaluate(ctx), b.evaluate(ctx));
            }
            case "math.max" -> {
                MolangValue a = !args.isEmpty() ? args.get(0) : ctx -> 0;
                MolangValue b = args.size() > 1 ? args.get(1) : ctx -> 0;
                yield ctx -> Math.max(a.evaluate(ctx), b.evaluate(ctx));
            }
            case "math.clamp" -> {
                MolangValue val = !args.isEmpty() ? args.get(0) : ctx -> 0;
                MolangValue min = args.size() > 1 ? args.get(1) : ctx -> 0;
                MolangValue max = args.size() > 2 ? args.get(2) : ctx -> 0;
                yield ctx -> Math.max(min.evaluate(ctx), Math.min(max.evaluate(ctx), val.evaluate(ctx)));
            }
            case "math.lerp" -> {
                MolangValue a = !args.isEmpty() ? args.get(0) : ctx -> 0;
                MolangValue b = args.size() > 1 ? args.get(1) : ctx -> 0;
                MolangValue t = args.size() > 2 ? args.get(2) : ctx -> 0;
                yield ctx -> {
                    double av = a.evaluate(ctx);
                    double bv = b.evaluate(ctx);
                    double tv = t.evaluate(ctx);
                    return av + (bv - av) * tv;
                };
            }
            case "math.mod" -> {
                MolangValue a = !args.isEmpty() ? args.get(0) : ctx -> 0;
                MolangValue b = args.size() > 1 ? args.get(1) : ctx -> 0;
                yield ctx -> {
                    double bv = b.evaluate(ctx);
                    return bv == 0 ? 0 : a.evaluate(ctx) % bv;
                };
            }
            case "math.pi" -> ctx -> Math.PI;
            case "math.random" -> {
                MolangValue min = !args.isEmpty() ? args.get(0) : ctx -> 0;
                MolangValue max = args.size() > 1 ? args.get(1) : ctx -> 1;
                yield ctx -> {
                    double minV = min.evaluate(ctx);
                    double maxV = max.evaluate(ctx);
                    return minV + Math.random() * (maxV - minV);
                };
            }
            case "math.to_deg" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.toDegrees(arg.evaluate(ctx));
            }
            case "math.to_rad" -> {
                MolangValue arg = args.isEmpty() ? ctx -> 0 : args.getFirst();
                yield ctx -> Math.toRadians(arg.evaluate(ctx));
            }
            default -> {
                LOGGER.fine("Unknown Molang function: " + name);
                yield ctx -> 0;
            }
        };
    }

    private static MolangValue parseVariable(String name) {
        String normalized = MolangContext.normalizeQueryName(name);

        MolangValue builtin = switch (normalized) {
            case "q.anim_time" -> MolangContext::getAnimTime;
            case "q.life_time" -> ctx -> ctx.getAnimTime();
            case "q.frame_alpha" -> MolangContext::getPartialTick;
            case "q.ground_speed" -> MolangContext::getGroundSpeed;
            case "q.vertical_speed" -> MolangContext::getVerticalSpeed;
            case "q.health" -> MolangContext::getHealth;
            case "q.max_health" -> MolangContext::getMaxHealth;
            case "q.is_on_ground" -> ctx -> ctx.isOnGround() ? 1 : 0;
            case "q.is_in_water" -> ctx -> ctx.isInWater() ? 1 : 0;
            case "q.is_moving" -> ctx -> ctx.isMoving() ? 1 : 0;
            case "q.hurt_time" -> MolangContext::getHurtTime;
            case "q.death_time" -> MolangContext::getDeathTime;
            case "pi" -> ctx -> Math.PI;
            case "e" -> ctx -> Math.E;
            default -> null;
        };

        return ctx -> {
            MolangValue override = ctx.getQueryValue(normalized);
            if (override != null) {
                return override.evaluate(ctx);
            }

            if (builtin != null) {
                return builtin.evaluate(ctx);
            }

            LOGGER.fine("Unknown Molang variable: " + name);
            return 0;
        };
    }

    private static class ParseState {
        private final String expression;
        private int position;

        ParseState(String expression) {
            this.expression = expression;
            this.position = 0;
        }

        boolean hasMore() {
            return position < expression.length();
        }

        char peek() {
            return expression.charAt(position);
        }

        void advance() {
            position++;
        }

        void advance(int count) {
            position += count;
        }

        String remaining() {
            return expression.substring(position);
        }

        void skipWhitespace() {
            while (hasMore() && Character.isWhitespace(peek())) {
                advance();
            }
        }
    }
}

