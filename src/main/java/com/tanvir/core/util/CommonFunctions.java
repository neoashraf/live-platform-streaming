package com.tanvir.core.util;

import com.google.gson.*;
import com.tanvir.core.util.enums.DateTimeFormatterPattern;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
@Slf4j
public class CommonFunctions {
	public String buildGsonBuilder(Object object) {
		return buildGsonWithInstant(object).toJson(object);
	}

	public Gson buildGson(Object object) {
		DateTimeFormatter formater = DateTimeFormatter.ofPattern(DateTimeFormatterPattern.DATE_TIME.getValue());
		return new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class,
						(JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString(), formater))
				.registerTypeAdapter(LocalDateTime.class,
						(JsonSerializer<LocalDateTime>) (localDateTime, type, jsonSerializationContext) ->
								new JsonPrimitive(localDateTime.format(formater)))
				.registerTypeAdapter(LocalDate.class,
						(JsonDeserializer<LocalDate>) (json, typeOfT, context) -> LocalDate.parse(json.getAsString(),
								DateTimeFormatter.ofPattern("yyyy-MM-dd")))
				.registerTypeAdapter(LocalDate.class,
						(JsonSerializer<LocalDate>) (localDateTime, type, jsonSerializationContext) ->
								new JsonPrimitive(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
				.setPrettyPrinting().create();
	}


	public Gson buildGsonWithInstant(Object object) {
		return new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class,
						(JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
								LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ofPattern(DateTimeFormatterPattern.DATE_TIME.getValue())))
				.registerTypeAdapter(LocalDateTime.class,
						(JsonSerializer<LocalDateTime>) (localDateTime, type, jsonSerializationContext) ->
								new JsonPrimitive(localDateTime.format(DateTimeFormatter.ofPattern(DateTimeFormatterPattern.DATE_TIME.getValue()))))
				.registerTypeAdapter(LocalDate.class,
						(JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
								LocalDate.parse(json.getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd")))
				.registerTypeAdapter(LocalDate.class,
						(JsonSerializer<LocalDate>) (localDateTime, type, jsonSerializationContext) ->
								new JsonPrimitive(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
				.registerTypeAdapter(Instant.class,
						(JsonDeserializer<Instant>) (json, typeOfT, context) ->
								Instant.parse(json.getAsString()))
				.registerTypeAdapter(Instant.class,
						(JsonSerializer<Instant>) (instant, type, jsonSerializationContext) ->
								new JsonPrimitive(instant.toString()))
				.setPrettyPrinting()
				.create();
	}

}
