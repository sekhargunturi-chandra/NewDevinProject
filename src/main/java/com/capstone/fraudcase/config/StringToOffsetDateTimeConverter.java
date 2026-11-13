package com.capstone.fraudcase.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * HTML datetime-local inputs (and user-typed values) carry no UTC offset.
 * Parse them as UTC rather than failing the whole intake form.
 */
@Component
public class StringToOffsetDateTimeConverter implements Converter<String, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(String source) {
        try {
            return OffsetDateTime.parse(source.trim());
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(source.trim()).atOffset(ZoneOffset.UTC);
        }
    }
}
