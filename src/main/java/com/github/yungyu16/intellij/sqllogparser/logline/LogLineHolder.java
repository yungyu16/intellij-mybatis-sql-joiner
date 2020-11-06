package com.github.yungyu16.intellij.sqllogparser.logline;

import com.google.common.base.Strings;

import java.util.Collections;
import java.util.List;

public class LogLineHolder {
    private final String statementLine;
    private String parameterLine;

    private LogLineHolder(String statementLine, String parameterLine) {
        this.statementLine = statementLine;
        this.parameterLine = parameterLine;
    }

    public static LogLineHolder valueOf(String statementLine) {
        return new LogLineHolder(statementLine, null);
    }

    public String getStatementLine() {
        return statementLine;
    }

    public List<String> getSqlParameters() {
        if (Strings.isNullOrEmpty(parameterLine)) {
            return Collections.emptyList();
        }
        return LogLineParser.parseParameter(parameterLine);
    }

    public void setParameterLine(String parameterLine) {
        this.parameterLine = parameterLine;
    }

    public String formattedSqlStatement() {
        String sql = LogLineParser.formatStatement(statementLine, getSqlParameters());
        if (!sql.endsWith(";")) {
            sql = sql + ";";
        }
        return sql;
    }
}
