package zty.practise.springrabbit.ack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ConsumerAckServer {

	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(ConsumerAckServer.class);
		
		//autoAck
		String path = "classpath:ack/amqp-consume-autoack.xml";
		
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(path);
		
		logger.info("Application Started.");
	}
}
