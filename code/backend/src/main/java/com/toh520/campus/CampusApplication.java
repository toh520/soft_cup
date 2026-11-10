package com.toh520.campus;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CampusApplication {
  public static void main(String[] args) throws Exception {
    Files.createDirectories(Path.of("data"));
    SpringApplication.run(CampusApplication.class, args);
  }
}
