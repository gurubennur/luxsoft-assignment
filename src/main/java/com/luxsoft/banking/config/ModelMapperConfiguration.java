package com.luxsoft.banking.config;

import com.luxsoft.banking.model.ApiModels.CreateAccountRequest;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.record.RecordModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures mappings between request, entity, and response models. */
@Configuration
public class ModelMapperConfiguration {
    /**
     * Creates a mapper for private entity fields and immutable Java record DTOs.
     * Field matching handles the request and entity properties with the same name.
     *
     * @return configured application-wide mapper
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.registerModule(new RecordModule());
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(AccessLevel.PRIVATE)
                .setMethodAccessLevel(AccessLevel.PACKAGE_PRIVATE);
        return modelMapper;
    }
}
