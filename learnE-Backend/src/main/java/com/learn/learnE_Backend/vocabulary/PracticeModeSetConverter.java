package com.learn.learnE_Backend.vocabulary;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

/** Stores a set of practice modes as a comma-separated list in one column. */
@Converter
public class PracticeModeSetConverter implements AttributeConverter<EnumSet<PracticeMode>, String> {

    @Override
    public String convertToDatabaseColumn(EnumSet<PracticeMode> modes) {
        if (modes == null || modes.isEmpty()) {
            return null;
        }
        return modes.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public EnumSet<PracticeMode> convertToEntityAttribute(String value) {
        EnumSet<PracticeMode> modes = EnumSet.noneOf(PracticeMode.class);
        if (value == null || value.isBlank()) {
            return modes;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                // Ignore names retired by a later release rather than failing to load the row.
                .filter(PracticeModeSetConverter::isKnownMode)
                .map(PracticeMode::valueOf)
                .forEach(modes::add);
        return modes;
    }

    private static boolean isKnownMode(String name) {
        return Arrays.stream(PracticeMode.values()).anyMatch(mode -> mode.name().equals(name));
    }
}
