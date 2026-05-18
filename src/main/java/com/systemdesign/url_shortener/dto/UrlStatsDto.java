package com.systemdesign.url_shortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlStatsDto {
    private String shortCode;
    private String longUrl;
    private Long clickCount;
    private LocalDateTime createdAt;

}
