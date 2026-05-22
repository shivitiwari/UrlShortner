package com.systemdesign.url_shortener.service;

import com.systemdesign.url_shortener.constant.Constant;
import com.systemdesign.url_shortener.dao.UrlDao;
import com.systemdesign.url_shortener.entity.Url;
import com.systemdesign.url_shortener.exception.InvalidUrlException;
import com.systemdesign.url_shortener.exception.ShortCodeAlreadyExistsException;
import com.systemdesign.url_shortener.exception.UrlExpiredException;
import com.systemdesign.url_shortener.exception.UrlNotFoundException;
import com.systemdesign.url_shortener.util.Base62Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;

@Service
public class UrlService {

    private static final Logger logger = LoggerFactory.getLogger(UrlService.class);
    private static final String BASE_URL = Constant.BASE_URL;

    private final UrlDao urlDAO;
    private final Base62Encoder base62Encoder;

    public UrlService(UrlDao urlDAO, Base62Encoder base62Encoder) {
        this.urlDAO = urlDAO;
        this.base62Encoder = base62Encoder;
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    //`shortenUrl(String longUrl)` - Generate short code, save, return short URL
    public String shortenUrl(String longUrl, Integer expirationDays) {
        return shortenUrl(longUrl, expirationDays, null);
    }

    public String shortenUrl(String longUrl, Integer expirationDays, String customCode) {
        if (!isValidUrl(longUrl)) {
            throw new InvalidUrlException(longUrl);
        }

        // Determine the short code to use
        String shortCode;
        if (customCode != null && !customCode.isBlank()) {
            String candidateShortUrl = BASE_URL + customCode;
            if (urlDAO.existsByShortUrl(candidateShortUrl)) {
                throw new ShortCodeAlreadyExistsException(customCode);
            }
            shortCode = customCode;
        } else {
            shortCode = null; // will be generated after DB save
        }

        Url url = new Url();
        url.setOriginalUrl(longUrl);
        url.setCreatedAt(LocalDate.now());
        int days = (expirationDays != null && expirationDays > 0) ? expirationDays : Constant.DEFAULT_EXPIRATION_DAYS;
        url.setExpiresAt(LocalDate.now().plusDays(days));
        url.setAccessCount(0);
        Url savedUrl = urlDAO.save(url);

        if (shortCode == null) {
            shortCode = base62Encoder.encode(savedUrl.getId());
        }

        savedUrl.setShortUrl(BASE_URL + shortCode);
        urlDAO.save(savedUrl);
        logger.info("URL shortened: {} -> {}", longUrl, savedUrl.getShortUrl());
        return savedUrl.getShortUrl();
    }

    //`getLongUrl(String shortCode)` - Retrieve original URL
    //@Cacheable(value = "urls", key = "#shortCode")
    @Cacheable(value = "urls", key = "#shortCode")
    public String getLongUrl(String shortCode) {
        long id = base62Encoder.decode(shortCode);
        Url url = urlDAO.findById(id).orElseThrow(() -> new UrlNotFoundException(shortCode));
        if (url.getExpiresAt().isBefore(LocalDate.now())) {
            logger.warn("URL expired: {}", url.getOriginalUrl());
            throw new UrlExpiredException(shortCode);
        }
        url.setAccessCount(url.getAccessCount() + 1);
        urlDAO.save(url);
        logger.info("URL accessed: {} -> {}", shortCode, url.getOriginalUrl());
        return url.getOriginalUrl();
    }

    // Return click count, creation date, expiration date
    public String getUrlStats(String shortCode) {
        long id = base62Encoder.decode(shortCode);
        Url url = urlDAO.findById(id).orElseThrow(() -> new UrlNotFoundException(shortCode));
        return String.format("Original URL: %s, Created At: %s, Expires At: %s, Access Count: %d",
                url.getOriginalUrl(), url.getCreatedAt(), url.getExpiresAt(), url.getAccessCount());
    }
}