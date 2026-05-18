package com.systemdesign.url_shortener.dao;

import com.systemdesign.url_shortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlDao extends JpaRepository<Url, Long> {

}
