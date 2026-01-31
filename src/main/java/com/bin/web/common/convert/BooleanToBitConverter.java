package com.bin.web.common.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BooleanToBitConverter implements AttributeConverter<Boolean, Boolean> {
    @Override
    public Boolean convertToDatabaseColumn(Boolean attribute) {
        return attribute != null ? attribute : Boolean.FALSE;
    }

    @Override
    public Boolean convertToEntityAttribute(Boolean dbData) {
        return dbData;
    }
}
