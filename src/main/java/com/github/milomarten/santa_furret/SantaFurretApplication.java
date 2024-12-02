package com.github.milomarten.santa_furret;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class SantaFurretApplication {

	public static void main(String[] args) {
		SpringApplication.run(SantaFurretApplication.class, args);
	}

}
