package com.github.yungyu16.intellij.sqllogparser.logline;

import java.util.Optional;
import java.util.function.Function;

public class LogLineCombiner implements Function<String, LogLineHolder> {
    private LogLineHolder logHolderCombiner = null;

    @Override
    public LogLineHolder apply(String line) {
        Optional<String> statementLineOpt = LogLineParser.extractStatementLine(line);
        Optional<String> parameterLineOpt = LogLineParser.extractParameterLine(line);
        boolean statementLinePresentFlag = statementLineOpt.isPresent();
        boolean parameterLinePresentFlag = parameterLineOpt.isPresent();
        if (statementLinePresentFlag && parameterLinePresentFlag) {
            return null;
        }
        if (!statementLinePresentFlag && !parameterLinePresentFlag) {
            return null;
        }
        if (statementLinePresentFlag) {
            return swapWithStatementLine(statementLineOpt.get());
        }
        return swapWithParameterLine(parameterLineOpt.get());
    }

    private LogLineHolder swapWithStatementLine(String statementLine) {
        LogLineHolder logLineHolderTemp = this.logHolderCombiner;
        if (logLineHolderTemp == null) {
            logHolderCombiner = LogLineHolder.valueOf(statementLine);
        }
        return logLineHolderTemp;
    }

    private LogLineHolder swapWithParameterLine(String parameterLine) {
        LogLineHolder logLineHolderTemp = this.logHolderCombiner;
        if (logLineHolderTemp != null) {
            logLineHolderTemp.setParameterLine(parameterLine);
            this.logHolderCombiner = null;
            return logLineHolderTemp;
        }
        return null;
    }

    public Optional<LogLineHolder> latestLogHolder() {
        if (logHolderCombiner == null) {
            return Optional.empty();
        }
        if (logHolderCombiner.getStatementLine() == null) {
            return Optional.empty();
        }
        return Optional.of(logHolderCombiner);
    }
}
