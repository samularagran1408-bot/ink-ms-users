package com.inklusport.users.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${cloudinary.url:}") String url,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        if (hasText(url)) {
            return new Cloudinary(url.trim());
        }
        if (hasText(cloudName) && hasText(apiKey) && hasText(apiSecret)) {
            return new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName.trim(),
                    "api_key", apiKey.trim(),
                    "api_secret", apiSecret.trim(),
                    "secure", true
            ));
        }
        return new Cloudinary(ObjectUtils.asMap("cloud_name", "disabled"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
