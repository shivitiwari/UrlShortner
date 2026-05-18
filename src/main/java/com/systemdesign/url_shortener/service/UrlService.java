package com.systemdesign.url_shortener.service;

import com.systemdesign.url_shortener.DAO.UrlDAO;
import com.systemdesign.url_shortener.DTO.Url;
import com.systemdesign.url_shortener.constant.Constant;
import com.systemdesign.url_shortener.util.Base62Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UrlService {

    private static final Logger logger= LoggerFactory.getLogger(UrlService.class);
    private static final String BASE_URL = Constant.BASE_URL;

    private final UrlDAO urlDAO;
    private final Base62Encoder base62Encoder;

    public UrlService(UrlDAO urlDAO, Base62Encoder base62Encoder) {
        this.urlDAO = urlDAO;
        this.base62Encoder = base62Encoder;
    }

    //`shortenUrl(String longUrl)` - Generate short code, save, return short URL
   public String shortenUrl(String longUrl) {
        try {
            Url url = new Url();
            url.setOriginalUrl(longUrl);
            url.setCreatedAt(LocalDate.now());
            url.setExpiresAt(LocalDate.now().plusDays(Constant.DEFAULT_EXPIRATION_DAYS));
            url.setAccessCount(0);
            Url savedUrl = urlDAO.save(url);
            String shortCode = base62Encoder.encode(savedUrl.getId());
            savedUrl.setShortUrl(BASE_URL + shortCode);
            urlDAO.save(savedUrl);
            logger.info("URL shortened: {} -> {}", longUrl, savedUrl.getShortUrl());
            return savedUrl.getShortUrl();
        } catch (Exception e) {
            logger.error("Error shortening URL: {}", e.getMessage());
            throw e; // or return error response
        }
    }

    //`getLongUrl(String shortCode)` - Retrieve original URL
   public String getLongUrl(String shortCode) {
        try {
            long id = base62Encoder.decode(shortCode);
            Url url = urlDAO.findById(id).orElseThrow(() -> new RuntimeException("URL not found"));
            if (url.getExpiresAt().isBefore(LocalDate.now())) {
                logger.warn("URL expired: {}", url.getOriginalUrl());
                throw new RuntimeException("URL expired");
            }
            url.setAccessCount(url.getAccessCount() + 1);
            urlDAO.save(url);
            logger.info("URL accessed: {} -> {}", shortCode, url.getOriginalUrl());
            return url.getOriginalUrl();
        } catch (Exception e) {
            logger.error("Error retrieving URL: {}", e.getMessage());
            throw e; // or return error response
        }
    }
}