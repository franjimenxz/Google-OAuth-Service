package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;


@Configuration
public class SessionCookieConfig {


    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer s = new DefaultCookieSerializer();
        s.setCookieName("JSESSIONID");   // o "SESSION"
        s.setSameSite("None");           // requerido cross-site
        s.setUseSecureCookie(true);      // HTTPS obligatorio
        s.setUseHttpOnlyCookie(true);
        s.setCookiePath("/");
        return s;
    }
}