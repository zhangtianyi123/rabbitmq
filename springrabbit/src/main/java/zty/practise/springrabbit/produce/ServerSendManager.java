package zty.practise.springrabbit.produce;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("serverSendManager")
public class ServerSendManager {
	
	private Logger logger = LoggerFactory.getLogger(ServerSendManager.class);
	
	@Autowired
	private AmqpTemplate amqpTemplate;
	
	public void sendMessage() {
		try {
			 RequestEntity entity = new RequestEntity();
			 entity.setEventName(LocalDateTime.now().toString());
			 entity.setLotName("FZ20190308X01");
			 entity.setProcName("PRESSURETEST");
			 entity.setReqId("requestQueue_threads-223_0000113_45118");
			 amqpTemplate.convertAndSend(entity);
			 logger.info("send message...");
		} catch (Exception e) {
		}
		
	}
	
}
