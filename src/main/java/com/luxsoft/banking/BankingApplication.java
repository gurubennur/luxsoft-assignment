package com.luxsoft.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankingApplication {
    /**
     * Starts the banking transaction processor.
     */
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
    }
}
