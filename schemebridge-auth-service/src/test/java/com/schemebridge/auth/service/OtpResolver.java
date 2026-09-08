package com.schemebridge.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class OtpResolver {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java OtpResolver <user_email>");
            System.exit(1);
        }

        String email = args[0].trim().toLowerCase();

        // 1. Fetch latest OTP hash from Oracle using SQL*Plus
        ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                "echo 'SET PAGESIZE 0 FEEDBACK OFF LINESIZE 200' " +
                "'SELECT ov.otp_hash FROM otp_verification ov JOIN users u ON ov.user_id = u.id WHERE LOWER(u.email) = ''" + email + "'' AND ov.verified_at IS NULL AND ov.expires_at > SYSDATE ORDER BY ov.created_at DESC;' " +
                "| sqlplus -s SYSTEM/system@localhost:1521/XEPDB1");
        Process proc = pb.start();

        String otpHash = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("$2a$") || line.startsWith("$2b$") || line.startsWith("$2y$")) {
                    otpHash = line;
                    break;
                }
            }
        }

        if (otpHash == null) {
            System.err.println("No unverified OTP hash found for " + email);
            System.exit(1);
        }

        // 2. Parallel search for matching 6-digit PIN
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        final String targetHash = otpHash;

        long start = System.currentTimeMillis();
        OptionalInt match = IntStream.range(0, 1_000_000).parallel()
                .filter(i -> {
                    String pin = String.format("%06d", i);
                    return encoder.matches(pin, targetHash);
                })
                .findFirst();

        long duration = System.currentTimeMillis() - start;

        if (match.isPresent()) {
            String resolvedOtp = String.format("%06d", match.getAsInt());
            System.out.println(resolvedOtp);
        } else {
            System.err.println("No match found in 000000..999999");
            System.exit(1);
        }
    }
}
