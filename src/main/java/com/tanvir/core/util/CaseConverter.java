package com.tanvir.core.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
@Slf4j
public class CaseConverter {
    public String snakeToCamel(String input) {
        Pattern pattern = Pattern.compile("_(.)");
        Matcher matcher = pattern.matcher(input);
        StringBuilder builder = new StringBuilder(input);

        while (matcher.find()) {
            builder.replace(matcher.start(), matcher.end(), matcher.group(1).toUpperCase());
            matcher = pattern.matcher(builder.toString());
        }

        return builder.toString();
    }

    public String camelToSnake(String input) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return input.replaceAll(regex, replacement).toLowerCase();
    }
}
