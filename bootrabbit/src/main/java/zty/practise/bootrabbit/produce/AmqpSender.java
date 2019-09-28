package zty.practise.bootrabbit.produce;


import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

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
	
	@Scheduled(fixedRate = 3000)
    public void send() {
        this.rabbitTemplate.convertAndSend(exchangeName, routingKey, (count++) + "");
        log.info("send message...");
    }
}
