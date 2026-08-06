package com.schemebridge.config;

import com.schemebridge.enums.RoleEnum;
import org.bson.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongoRoleEnumConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new RoleEnumDocumentReadConverter(),
                new RoleEnumWriteConverter()
        ));
    }

    private static final class RoleEnumDocumentReadConverter implements Converter<Document, RoleEnum> {
        @Override
        public RoleEnum convert(Document source) {
            if (source == null) {
                return null;
            }
            Object name = source.get("name");
            if (name instanceof String roleName) {
                return RoleEnum.valueOf(roleName.trim().toUpperCase());
            }
            if (name instanceof RoleEnum roleEnum) {
                return roleEnum;
            }
            Object roleValue = source.get("role");
            if (roleValue instanceof String roleString) {
                return RoleEnum.valueOf(roleString.trim().toUpperCase());
            }
            if (roleValue instanceof RoleEnum roleEnum) {
                return roleEnum;
            }
            throw new IllegalArgumentException("Unsupported role value: " + source);
        }
    }

    private static final class RoleEnumWriteConverter implements Converter<RoleEnum, String> {
        @Override
        public String convert(RoleEnum source) {
            return source == null ? null : source.name();
        }
    }
}
