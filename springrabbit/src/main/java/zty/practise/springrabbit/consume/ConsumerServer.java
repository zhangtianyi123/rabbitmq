package zty.practise.springrabbit.consume;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class ConsumerServer {
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(ConsumerServer.class);
		
		String path = "classpath:amqp-consume.xml";
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(path);
		
		logger.info("Application Started.");
	}
}
