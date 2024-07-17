package com.tanvir.core.util.helper;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class HelperUtil implements Serializable {

    public static String getFormattedBigDecimal(BigDecimal bigDecimal) {
        if (bigDecimal == null) {
            return null;
        } else {
            if (bigDecimal.doubleValue() < 1000) {
                return format("##0.00", bigDecimal.doubleValue());
            } else {
                double belowThousand = bigDecimal.doubleValue() % 1000;
                int aboveThousand = (int) (bigDecimal.doubleValue() / 1000);
                return format("##", aboveThousand) + format("000.00", belowThousand);
            }
        }
    }


    public static BigDecimal amountIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.valueOf(0.00);
    }

    private static String format(String pattern, Object value) {
        return new DecimalFormat(pattern).format(value);
    }

    public static Boolean checkIfNullOrEmpty(final String s) {
        return s == null || s.trim().isEmpty();
    }

    public static <T> String getFieldValueFromObject(T obj, String field){
        AtomicReference<String> providedUserRole = new AtomicReference<>();
        try{
            Field declaredField = obj.getClass().getDeclaredField(field);
            declaredField.setAccessible(true);
            providedUserRole.set(declaredField.get(obj).toString());
        } catch (Exception ignored){
            providedUserRole.set("");
        }
        return providedUserRole.get();
    }

    public static <T> Boolean validateRequestDTO(T requestDTO, List<String> validatationFieldList) {
        boolean isValid = false;
        if(!validatationFieldList.isEmpty()){
            try {
                for (String field : validatationFieldList) {
                    Field declaredField = requestDTO.getClass().getDeclaredField(field);
                    declaredField.setAccessible(true);
                    if (HelperUtil.checkIfNullOrEmpty(declaredField.get(requestDTO).toString())) {
                        throw new Exception();
                    } else {
                        isValid = true;
                    }
                }
            } catch (Exception error) {
                isValid = false;
            }
        } else {
            isValid = true;
        }
        return isValid;
    }

}
