package tech.aiflowy.datacenter.utils;

import com.agentsflex.core.util.MapUtil;
import com.alibaba.fastjson2.JSONPath;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlTemplate {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}");

    private static final Map<String, SqlTemplate> TEMPLATE_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, JSONPath> JSONPATH_CACHE = new ConcurrentHashMap<>();

    private final String originalTemplate;
    private final List<Token> tokens;

    public SqlTemplate(String template) {
        this.originalTemplate = template == null ? "" : template;
        this.tokens = Collections.unmodifiableList(parseTemplate(this.originalTemplate));
    }

    public static SqlTemplate of(String template) {
        String value = template == null ? "" : template;
        return MapUtil.computeIfAbsent(TEMPLATE_CACHE, value, SqlTemplate::new);
    }

    public SqlInfo format(Map<String, Object> params) {

        if (params == null) {
            params = Collections.emptyMap();
        }

        StringBuilder sql = new StringBuilder(originalTemplate.length() + 64);

        List<Object> arguments = new ArrayList<>();

        for (Token token : tokens) {
            if (token.staticText) {
                sql.append(token.content);
                continue;
            }

            Object value = getValueByJsonPath(params, token.expression);
            switch (token.type) {
                case FRAGMENT:
                    if (value != null) {
                        if (!(value instanceof CharSequence)) {
                            throw new IllegalArgumentException("SQL fragment must be String. expression=" + token.expression);
                        }
                        sql.append(value);
                    }
                    break;

                case PARAMETER:
                    sql.append("?");
                    arguments.add(token.stringParameter ? value : convertNumericString(value));
                    break;

                case PARAMETER_LIST:
                    appendParameterList(sql, arguments, value);
                    break;
            }
        }

        return new SqlInfo(normalizeSql(sql.toString()), arguments);
    }

    private void appendParameterList(StringBuilder sql, List<Object> arguments, Object value) {

        Collection<?> collection = toCollection(value);

        if (collection == null || collection.isEmpty()) {
            sql.append("(null)");
            return;
        }

        sql.append("(");

        int index = 0;

        for (Object item : collection) {
            if (index++ > 0) {
                sql.append(",");
            }
            sql.append("?");
            arguments.add(convertNumericString(item));
        }

        sql.append(")");
    }

    private Object convertNumericString(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            return value;
        }
        String str = ((String) value).trim();
        if (str.isEmpty()) {
            return value;
        }

        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Long.parseLong(str);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException ignored) {
        }

        return value;
    }

    private Collection<?> toCollection(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Collection<?> collection) {
            return collection;
        }

        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            List<Object> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                list.add(Array.get(value, i));
            }

            return list;
        }

        throw new IllegalArgumentException("Value must be collection or array.");
    }

    private Object getValueByJsonPath(Map<String, Object> root, String expression) {

        try {
            String path = expression.startsWith("$") ? expression : "$." + expression;
            JSONPath jsonPath = MapUtil.computeIfAbsent(JSONPATH_CACHE, path, JSONPath::compile);
            return jsonPath.eval(root);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Token> parseTemplate(String template) {

        List<Token> result = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                result.add(Token.staticText(template.substring(lastEnd, matcher.start())));
            }

            String expression = matcher.group(1).trim();
            TokenType type;

            if (expression.endsWith("?")) {
                type = TokenType.PARAMETER;
                expression = expression.substring(0, expression.length() - 1).trim();
            } else if (expression.endsWith("*")) {
                type = TokenType.PARAMETER_LIST;
                expression = expression.substring(0, expression.length() - 1).trim();
            } else {

                type = TokenType.FRAGMENT;
            }

            result.add(Token.dynamic(expression, type));
            lastEnd = matcher.end();
        }

        if (lastEnd < template.length()) {
            result.add(Token.staticText(template.substring(lastEnd)));
        }

        postProcessQuotedParameters(result);

        return result;
    }

    private void postProcessQuotedParameters(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.staticText || token.type != TokenType.PARAMETER) {
                continue;
            }

            boolean hasLeftQuote = false;
            int leftTokenIndex = -1;
            if (i > 0) {
                Token leftToken = tokens.get(i - 1);
                if (leftToken.staticText && leftToken.content != null && leftToken.content.endsWith("'")) {
                    hasLeftQuote = true;
                    leftTokenIndex = i - 1;
                }
            }

            boolean hasRightQuote = false;
            int rightTokenIndex = -1;
            if (i < tokens.size() - 1) {
                Token rightToken = tokens.get(i + 1);
                if (rightToken.staticText && rightToken.content != null && rightToken.content.startsWith("'")) {
                    hasRightQuote = true;
                    rightTokenIndex = i + 1;
                }
            }

            if (hasLeftQuote && hasRightQuote) {
                Token leftToken = tokens.get(leftTokenIndex);
                tokens.set(leftTokenIndex, Token.staticText(leftToken.content.substring(0, leftToken.content.length() - 1)));

                Token rightToken = tokens.get(rightTokenIndex);
                tokens.set(rightTokenIndex, Token.staticText(rightToken.content.substring(1)));

                tokens.set(i, Token.dynamic(token.expression, token.type, true));
            }
        }
    }

    private String normalizeSql(String sql) {
        return sql.replaceAll("[ \\t]+", " ").replaceAll("\\n\\s+\\n", "\n").trim();
    }

    private enum TokenType {
        FRAGMENT, PARAMETER, PARAMETER_LIST
    }

    private static class Token {

        final boolean staticText;
        final String content;

        final String expression;
        final TokenType type;
        final boolean stringParameter;

        private Token(boolean staticText, String content, String expression, TokenType type, boolean stringParameter) {
            this.staticText = staticText;
            this.content = content;
            this.expression = expression;
            this.type = type;
            this.stringParameter = stringParameter;
        }

        static Token staticText(String text) {

            return new Token(true, text, null, null, false);
        }

        static Token dynamic(String expression, TokenType type) {

            return new Token(false, null, expression, type, false);
        }

        static Token dynamic(String expression, TokenType type, boolean stringParameter) {

            return new Token(false, null, expression, type, stringParameter);
        }
    }
}
