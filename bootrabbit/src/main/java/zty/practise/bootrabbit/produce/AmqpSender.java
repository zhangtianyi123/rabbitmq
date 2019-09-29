package zty.practise.bootrabbit.produce;

import java.time.LocalDateTime;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zty.practise.bootrabbit.consume.PressureTestService;
import zty.practise.bootrabbit.model.RequestEntity;

@Service("amqpSender")
@Slf4j
public class AmqpSender {

	@Autowired
	private AmqpTemplate rabbitTemplate;

	@Value("${rabbitmq.requestExchange}")
	private String exchangeName;

	@Value("${rabbitmq.requestRoutingkey}")
	private String routingKey;
	
	private static long count = 0;

	@Scheduled(fixedRate = 30)
	public void send() {
		RequestEntity entity = new RequestEntity();
		entity.setEventName(LocalDateTime.now().toString());
		entity.setLotName(RandomStringUtils.randomNumeric(5));
		entity.setProcName(RandomStringUtils.randomAscii(5));
		entity.setReqId((count++) + "");
		this.rabbitTemplate.convertAndSend(exchangeName, routingKey, entity);
		log.info("send message...");
	}
	
//	@Scheduled(fixedRate = 3000)
//	public void sendErrorMessage() {
//		try {
//			 RequestEntity entity = new RequestEntity();
//			 entity.setEventName(LocalDateTime.now().toString());
//			 entity.setLotName(null);
//			 entity.setProcName(null);
//			 entity.setReqId((count++) + "");
//			 this.rabbitTemplate.convertAndSend(exchangeName, routingKey, entity);
//			 log.info("send message...");
//		} catch (Exception e) {
//		}
//		
//	}
}
