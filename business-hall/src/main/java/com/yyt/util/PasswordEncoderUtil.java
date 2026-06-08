package com.yyt.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("Admin password (123456): " + encoder.encode("123456"));
        System.out.println("Clerk password (clerk123): " + encoder.encode("clerk123"));
    }
}
