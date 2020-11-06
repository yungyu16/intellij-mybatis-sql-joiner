package com.github.yungyu16.intellij.sqllogparser.logline;

import com.github.yungyu16.intellij.sqllogparser.error.TipException;
import com.google.common.base.Strings;
import com.google.common.primitives.Primitives;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.codec.binary.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CreatedDate: 2020/11/5
 * Author: songjialin
 */
public class LogLineParser {
    static final Logger log = Logger.getInstance(LogLineParser.class);
    static final String STATEMENT_PREFIX = "Preparing:";
    static final String PARAMETERS_PREFIX = "Parameters:";
    static final String PARAMETER_SEPARATOR = ",";
    static final String PARAMETER_PLACEHOLDER = "\\?";
    static final Pattern PARAMETER_PATTERN = Pattern.compile("(.*)\\((.+)\\)");
    static final Set<String> RAW_PARAMETER_TYPES = Primitives.allWrapperTypes().stream().map(Class::getSimpleName).collect(Collectors.toSet());

    public static String parse(String sqlLog) {
        if (StringUtil.isEmpty(sqlLog)) {
            throw new TipException("sql日志为空");
        }
        sqlLog = sqlLog.trim();
        if (StringUtil.isEmpty(sqlLog)) {
            throw new TipException("sql日志为空");
        }
        if (isAlreadyFormattedSqlStatement(sqlLog)) {
            log.debug("当前sql日志已经是完整的sql语句:" + sqlLog);
            return sqlLog;
        }
        try (ByteArrayInputStream sqlIn = new ByteArrayInputStream(StringUtils.getBytesUtf8(sqlLog));
             InputStreamReader transform = new InputStreamReader(sqlIn);
             BufferedReader sqlReader = new BufferedReader(transform)) {
            LogLineCombiner combiner = new LogLineCombiner();
            String collectSql = sqlReader.lines()
                    .filter(line -> !Strings.isNullOrEmpty(line))
                    .map(combiner)
                    .filter(Objects::nonNull)
                    .map(LogLineHolder::formattedSqlStatement)
                    .collect(Collectors.joining());
            return collectSql + (combiner.latestLogHolder()
                    .map(LogLineHolder::formattedSqlStatement)
                    .orElse(""));
        } catch (IOException e) {
            log.error("解析sql日志异常", e);
            throw new TipException("解析sql日志异常");
        }
    }

    static Optional<String> extractStatementLine(String logLine) {
        return extractPayload(logLine, STATEMENT_PREFIX);
    }

    static Optional<String> extractParameterLine(String logLine) {
        return extractPayload(logLine, PARAMETERS_PREFIX);
    }

    private static Optional<String> extractPayload(String logLine, String identifyPrefix) {
        int prefixIdx = logLine.indexOf(identifyPrefix);
        if (prefixIdx < 0) {
            return Optional.empty();
        }
        String payload = logLine.substring(prefixIdx + identifyPrefix.length());
        return Optional.of(payload);
    }

    static boolean isAlreadyFormattedSqlStatement(String sqlLog) {
        String finalSqlLog = sqlLog.toUpperCase().trim();
        return Stream.of("SELECT", "UPDATE", "DELETE", "INSERT").anyMatch(finalSqlLog::startsWith);
    }

    static List<String> parseParameter(String parameter) {
        if (StringUtil.isEmpty(parameter)) {
            return Collections.emptyList();
        }
        parameter = parameter.trim();
        return StringUtil.split(parameter, PARAMETER_SEPARATOR, true, false)
                .stream()
                .map(String::trim)
                .map(it -> {
                    if ("null".equals(it)) {
                        return Pair.<String, String>create("null", null);
                    }
                    Matcher matcher = PARAMETER_PATTERN.matcher(it);
                    if (!matcher.matches()) {
                        return Pair.create("", "");
                    }
                    return Pair.create(matcher.group(1), matcher.group(2));
                }).map(LogLineParser::formatParameter)
                .collect(Collectors.toList());
    }

    private static String formatParameter(Pair<String, String> paramInfo) {
        String val = paramInfo.getFirst();
        String type = paramInfo.getSecond();
        if (type == null || RAW_PARAMETER_TYPES.contains(type)) {
            return val;
        }
        return "'" + StringUtil.escapeChar(val, '\'') + "'";
    }

    static String formatStatement(String statement, List<String> params) {
        for (String param : params) {
            statement = statement.replaceFirst(PARAMETER_PLACEHOLDER, param);
        }
        return statement;
    }
}
