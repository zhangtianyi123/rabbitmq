package zty.practise.bootrabbit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;


@Configuration
public class AmqpConfig {

	@Value("${rabbitmq.requestQueue}")
	private String requestQueue;
	
	@Value("${rabbitmq.requestExchange}")
	private String requestExchange;
	
	@Value("${rabbitmq.requestRoutingkey}")
	private String binding;
	
	@Bean
    public Queue queue() {
        return new Queue(requestQueue);
    }
	
    @Bean
    TopicExchange exchange() {
        return new TopicExchange(requestExchange);
    }
    
    @Bean
    Binding bindingExchangeMessage(Queue queueMessage, TopicExchange exchange) {
        return BindingBuilder.bind(queueMessage).to(exchange).with(binding);
    }

}
