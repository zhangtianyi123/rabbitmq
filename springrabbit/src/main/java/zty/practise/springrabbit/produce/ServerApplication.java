package zty.practise.springrabbit.produce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServerApplication {
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(ServerApplication.class);
		
		String path = "classpath:amqp-bootstrap.xml";
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(path);
		
		logger.info("Application Started.");
	}
}
