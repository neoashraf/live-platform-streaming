package com.tanvir.core.util;

import com.google.gson.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
@Slf4j
public class CommonFunctions {
	public String buildGsonBuilder(Object object) {
		return buildGson(object).toJson(object);
	}
	
	public Gson buildGson(Object object) {
		return new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class,
						(JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString(),
								DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")))
				.registerTypeAdapter(LocalDateTime.class,
						(JsonSerializer<LocalDateTime>) (localDateTime, type, jsonSerializationContext) ->
								new JsonPrimitive(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))))
				.registerTypeAdapter(LocalDate.class,
						(JsonDeserializer<LocalDate>) (json, typeOfT, context) -> LocalDate.parse(json.getAsString(),
								DateTimeFormatter.ofPattern("yyyy-MM-dd")))
				.registerTypeAdapter(LocalDate.class,
						(JsonSerializer<LocalDate>) (localDateTime, type, jsonSerializationContext) ->
								new JsonPrimitive(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
				.setPrettyPrinting().create();
	}
	
	public double round(int scale, double amount, RoundingMode roundingMode) {
		return new BigDecimal(amount).setScale(scale, roundingMode).doubleValue();
	}

	public static <T> String getFieldValueByObjectAndFieldName(T object, String fieldName) {
		String fieldValue = null;
		try {
			Class<?> passbookClass = object.getClass();
			Field field = passbookClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			fieldValue = field.get(object) == null ? null : field.get(object).toString();
			log.info("field Name : {}, value of field : {}",fieldName, fieldValue);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			log.error("Error : Field : {} not found or inaccessible", fieldName);
		}
		return fieldValue;
	}

	public RoundingMode getRoundingMode(String roundingLogic) {
		RoundingMode roundingMode = null;
		switch (roundingLogic.toUpperCase()) {
			case "HALFUP" -> roundingMode = RoundingMode.HALF_UP;
			case "HALFDOWN" -> roundingMode = RoundingMode.HALF_DOWN;
			case "UP" -> roundingMode = RoundingMode.UP;
			case "DOWN" -> roundingMode = RoundingMode.DOWN;
		}
		return roundingMode;
	}
}
