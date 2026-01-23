package com.abc.foodwastemanagement.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customConfig() {
        
        return new OpenAPI().info(
            new Info().title("Food Waste Management App APIs")
                    .description("By S Nagarjuna")
                    .version("1.0.0")
        )
        .tags(Arrays.asList(
                new Tag().name("User APIs").description("Read, Update, Verify email, Forgot password, Reset password and Delete Users."),
                new Tag().name("Food Waste Collection Center APIs").description("Add, Delete, Update, View-All, View-Active and View-by-ID Food Waste Collection Centers."),
                new Tag().name("Food Waste Donation APIs").description("Create, Get all, get by ID and collect a food waste donation."),
                new Tag().name("Food Waste Donor APIs").description("Create, Get all, update and delete a food waste donor."),
                new Tag().name("Notification APIs").description("Get all, mark as read, mark all as read and get unread count of the notifications of an user."),
                new Tag().name("Google login API").description("Allows users to login through Google account.")
        ))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(new Components().addSecuritySchemes(
                    "bearerAuth", new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .in(SecurityScheme.In.HEADER)
                            .name("Authorization")
        ));
    
    }
}
