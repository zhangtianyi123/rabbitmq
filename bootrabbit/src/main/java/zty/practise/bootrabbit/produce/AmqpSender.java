package zty.practise.bootrabbit.produce;


import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

@Service("amqpSender")
public class AmqpSender {
	
    @Autowired
    private AmqpTemplate rabbitTemplate;
    
	@Value("${rabbitmq.requestExchange}")
	private String exchangeName;
	
	@Value("${rabbitmq.requestRoutingkey}")
	private String routingKey;
	
    public void send(String message) {
        this.rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
    }
}
