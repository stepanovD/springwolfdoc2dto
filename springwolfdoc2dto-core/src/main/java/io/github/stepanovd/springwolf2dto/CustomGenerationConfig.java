package io.github.stepanovd.springwolf2dto;

import org.jsonschema2pojo.AnnotationStyle;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.SourceType;

public class CustomGenerationConfig extends DefaultGenerationConfig {
    @Override
    public boolean isGenerateBuilders() {
        return true;
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.JSONSCHEMA;
    }

    @Override
    public AnnotationStyle getAnnotationStyle() {
        return AnnotationStyle.JACKSON;
    }

    @Override
    public String getDateTimeType() {
        return "java.time.LocalDateTime";
    }

    @Override
    public String getDateType() {
        return "java.time.LocalDate";
    }


    @Override
    public boolean isIncludeHashcodeAndEquals() {
        return false;
    }

    @Override
    public boolean isIncludeToString() {
        return false;
    }

    @Override
    public boolean isIncludeGeneratedAnnotation() {
        return true;
    }

    @Override
    public boolean isSerializable() {
        return true;
    }

    @Override
    public boolean isFormatDateTimes() {
        return true;
    }

    @Override
    public String getCustomDateTimePattern() {
        return "yyyy-MM-dd'T'HH:mm:ss";
    }
}
