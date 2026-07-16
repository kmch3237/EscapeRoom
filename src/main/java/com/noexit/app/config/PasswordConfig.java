package com.noexit.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 해시용 PasswordEncoder 빈 등록.
 * BCrypt = 단방향 해시(복호화 불가) + salt 자동 부여(같은 비번도 매번 다른 해시).
 * 기존 CRYPTPACK(양방향 암호화)을 대체.
 */
@Configuration
public class PasswordConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
