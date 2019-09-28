package zty.practise.springrabbit.produce;

import java.time.LocalDateTime;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import zty.practise.springrabbit.model.RequestEntity;

@Component("serverSendManager")
public class ServerSendManager {
	
	private Logger logger = LoggerFactory.getLogger(ServerSendManager.class);
	
	@Autowired
	private AmqpTemplate amqpTemplate;
	
	private static long count = 0;
	
	/**
	 * 发送正常的消息
	 * 在amqp-producer中定时任务调度
	 */
	public void sendMessage() {
		try {
			 RequestEntity entity = new RequestEntity();
			 entity.setEventName(LocalDateTime.now().toString());
			 entity.setLotName(RandomStringUtils.randomNumeric(5));
			 entity.setProcName(RandomStringUtils.randomAscii(5));
			 entity.setReqId((count++) + "");
			 amqpTemplate.convertAndSend(entity);
			 logger.info("send message...");
		} catch (Exception e) {
		}
	}
	
	/**
	 * 发送错误消息，脏数据，以测试消费
	 * 在amqp-producer中定时任务调度
	 */
	public void sendErrorMessage() {
		try {
			 RequestEntity entity = new RequestEntity();
			 entity.setEventName(LocalDateTime.now().toString());
			 entity.setLotName(null);
			 entity.setProcName(null);
			 entity.setReqId((count++) + "");
			 amqpTemplate.convertAndSend(entity);
			 logger.info("send message...");
		} catch (Exception e) {
		}
		
	}
	
}
