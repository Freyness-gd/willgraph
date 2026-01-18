package at.ac.tuwien.mogda.willgraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

  private BackendApplication() {

  }

  static void main(final String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }

}
