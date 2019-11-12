package zty.practise.cloudrabbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RabbitApp {

	public static void main(String[] args) {
		SpringApplication.run(RabbitApp.class, args);
	}
}
