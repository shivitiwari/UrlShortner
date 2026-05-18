package com.systemdesign.url_shortener.DAO;

import com.systemdesign.url_shortener.DTO.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlDAO extends JpaRepository<Url, Long> {

}
