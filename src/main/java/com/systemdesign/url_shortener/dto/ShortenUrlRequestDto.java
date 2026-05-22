package com.systemdesign.url_shortener.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShortenUrlRequestDto {

    @NotBlank(message = "longUrl must not be blank")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String longUrl;

    @Min(value = 1, message = "expirationDays must be at least 1")
    private Integer expirationDays;

    @Pattern(
        regexp = "^[a-zA-Z0-9]{4,10}$",
        message = "customCode must be 4-10 alphanumeric characters (a-z, A-Z, 0-9)"
    )
    private String customCode;
}
