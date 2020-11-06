package com.github.yungyu16.mybatissql;

import com.github.yungyu16.mybatissql.error.TipException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.primitives.Primitives;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.codec.binary.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CreatedDate: 2020/11/5
 * Author: songjialin
 */
public class MybatisSqlLogJoiner {
    private static final Logger log = Logger.getInstance(MybatisSqlLogJoiner.class);
    private static final String STATEMENT_PREFIX = "Preparing:";
    private static final String PARAMETERS_PREFIX = "Parameters:";
    private static final int STATEMENT_PREFIX_LENGTH = STATEMENT_PREFIX.length();
    private static final int PARAMETERS_PREFIX_LENGTH = PARAMETERS_PREFIX.length();
    private static final String PARAMETER_SEPARATOR = ",";
    private static final String PARAMETER_PLACEHOLDER = "\\?";
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(.*)\\((.+)\\)");
    // private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(PARAMETER_PLACEHOLDER);
    private static final Set<String> RAW_PARAMETER_TYPES = Primitives.allWrapperTypes().stream().map(Class::getSimpleName).collect(Collectors.toSet());

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

            String statementLine;
            int statementStartIndex = -1;
            //定位语句行
            while ((statementLine = sqlReader.readLine()) != null) {
                statementStartIndex = statementLine.indexOf(STATEMENT_PREFIX);
                if (statementStartIndex >= 0) {
                    break;
                }
            }
            if (statementStartIndex < 0) {
                throw new TipException("无法定位语句前缀:" + STATEMENT_PREFIX);
            }
            List<String> params = Collections.emptyList();
            //定位参数行
            String parameterLine = sqlReader.readLine();
            if (parameterLine != null) {
                int parameterStartIndex = parameterLine.indexOf(PARAMETERS_PREFIX);
                if (parameterStartIndex < 0) {
                    throw new TipException("无法定位参数前缀:" + PARAMETERS_PREFIX);
                }
                String parameter = parameterLine.substring(parameterStartIndex + PARAMETERS_PREFIX_LENGTH);
                params = parseParameter(parameter);
            }
            String statement = statementLine.substring(statementStartIndex + STATEMENT_PREFIX_LENGTH);
            if (Strings.isNullOrEmpty(statement)) {
                throw new TipException("解析得到的sql语句为空");
            }
            statement = statement.trim();
            return formatStatement(statement, params);
        } catch (IOException e) {
            log.error("解析sql日志异常", e);
            throw new TipException("解析sql日志异常");
        }
    }

    private static boolean isAlreadyFormattedSqlStatement(String sqlLog) {
        final String finalSqlLog = sqlLog.toUpperCase().trim();
        return Stream.of("SELECT", "UPDATE", "DELETE", "INSERT").anyMatch(finalSqlLog::startsWith);
    }

    private static List<String> parseParameter(String parameter) {
        if (StringUtil.isEmpty(parameter)) {
            return Collections.emptyList();
        }
        parameter = parameter.trim();
        return StringUtil.split(parameter, PARAMETER_SEPARATOR, true, false)
                .stream()
                .map(String::trim)
                .map(it -> {
                    Matcher matcher = PARAMETER_PATTERN.matcher(it);
                    if (!matcher.matches()) {
                        return Pair.create("", "");
                    }
                    return Pair.create(matcher.group(1), matcher.group(2));
                }).map(MybatisSqlLogJoiner::formatParameter)
                .collect(Collectors.toList());
    }

    private static String formatParameter(Pair<String, String> paramInfo) {
        String val = paramInfo.getFirst();
        String type = paramInfo.getSecond();
        if (RAW_PARAMETER_TYPES.contains(type)) {
            return val;
        }
        return "'" + StringUtil.escapeChar(val, '\'') + "'";
    }

    private static String formatStatement(String statement, List<String> params) {
        for (String param : params) {
            statement = statement.replaceFirst(PARAMETER_PLACEHOLDER, param);
        }
        return statement;
    }

    public static void main(String[] args) {
        Pattern p = Pattern.compile("cat");
        Matcher m = p.matcher("one cat two cats in the yard");
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "dog");
        }
        m.appendTail(sb);
        System.out.println(sb.toString());
        ArrayList<String> params = Lists.newArrayList("(Integer)");
        params.add("1(Integer)");
        params.add("2020-10-30 18:09:00.058(Timestamp)");
        params.forEach(it -> {
            System.out.println("》》》》》" + it);
            Matcher matcher = PARAMETER_PATTERN.matcher(it);
            System.out.println(matcher.matches());
            try {
                System.out.println(matcher.group(0));
                System.out.println(matcher.group(1));
                System.out.println(matcher.group(2));
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("---------");
        });
        String result = parse("Preparing: SELECT id,is_deleted,created_time,updated_time FROM t_vip_coupon_used_info WHERE ( transaction_status = ? and created_time >= ? )\n" +
                "2020-11-04 18:09:00:060 DEBUG [t: Thread-61] cn.xiaoshidai.web.app.boot.dao.repository.VipCouponUsedInfoRepository.selectByExample-159 accId[] [202011041809007731fb91b0f14a51b05fb0d8291e76d8] ==> Parameters: 1(Integer), 2020-10-30 18:09:00.058(Timestamp)");
        System.out.println(result);
    }
}
