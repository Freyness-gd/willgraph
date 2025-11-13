package at.ac.tuwien.mogda.willgraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

  private BackendApplication() {

  }

  static void main(final String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }

}
