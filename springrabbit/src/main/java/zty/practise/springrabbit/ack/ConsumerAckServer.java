package zty.practise.springrabbit.ack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 注释选定一个path 启动不同的ack模式
 * @author zhangtianyi
 *
 */
public class ConsumerAckServer {

	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(ConsumerAckServer.class);
		
		//autoAck
//		String path = "classpath:ack/amqp-consume-autoack.xml";
		
		//noneAck
//		String path = "classpath:ack/amqp-consume-noneack.xml";
		
		//manualack
		String path = "classpath:ack/amqp-consume-manualack.xml";
		
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(path);
		
		logger.info("Application Started.");
	}
}
