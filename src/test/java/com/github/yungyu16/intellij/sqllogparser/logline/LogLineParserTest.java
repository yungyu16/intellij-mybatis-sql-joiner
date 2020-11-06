package com.github.yungyu16.intellij.sqllogparser.logline;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.StreamUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CreatedDate: 2020/11/6
 * Author: songjialin
 */
public class LogLineParserTest {

    @Test
    public void parse() throws IOException {
        InputStream input = LogLineParserTest.class.getResourceAsStream("/sqllog.txt");
        String log = StreamUtil.readText(input, Charsets.UTF_8);
        String result = LogLineParser.parse(log);
        System.out.println(result);
    }

    @Test
    public void regex() {
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
        params.add("null");
        params.add("2020-10-30 18:09:00.058(Timestamp)");
        params.forEach(it -> {
            System.out.println("》》》》》" + it);
            Matcher matcher = LogLineParser.PARAMETER_PATTERN.matcher(it);
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
    }
}
