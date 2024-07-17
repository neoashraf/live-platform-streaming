package com.tanvir.core.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BaseToString {

    @Override
    public String toString() {
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
                .setPrettyPrinting().create().toJson(this);
    }
}
