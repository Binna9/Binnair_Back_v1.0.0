package com.bin.web.common.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Boolean 값 Y | N 치환
 */
@Converter(autoApply = true)
public class BooleanToYNConverter implements AttributeConverter<Boolean,String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return "N";
        }
        return attribute ? "Y" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        return "Y".equalsIgnoreCase(dbData);
    }
}
