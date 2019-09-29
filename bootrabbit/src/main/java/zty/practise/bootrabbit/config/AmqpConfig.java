package zty.practise.bootrabbit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;

/**
 * 使用构建者模式来创建queue Exchange... 参数属性通过Map传递（比如死信队列）
 * 如果声明的属性和broker实体已经建立的不一样，那么启动会报错
 * 
 * @author zhangtianyi
 *
 */
@Configuration
public class AmqpConfig {

	@Value("${rabbitmq.requestQueue}")
	private String requestQueue;

	@Value("${rabbitmq.requestExchange}")
	private String requestExchange;

	@Value("${rabbitmq.requestRoutingkey}")
	private String binding;

	// 死信
	@Value("${rabbitmq.deadExchange}")
	private String deadExchange;

	@Value("${rabbitmq.deadQueue}")
	private String deadQueue;

	@Value("${rabbitmq.deadRoutingkey}")
	private String deadbinding;

	@Bean("requestQueue")
	public Queue requestQueue() {
		Map<String, Object> args = new HashMap<>(2);
		args.put("x-dead-letter-exchange", deadExchange);
		args.put("x-dead-letter-routing-key", deadbinding);
		return QueueBuilder.durable(requestQueue).withArguments(args).build();
	}

	@Bean("requestExchange")
	public TopicExchange requestExchange() {
		return new TopicExchange(requestExchange);
	}

	@Bean("deadQueue")
	public Queue deadQueue() {
		return new Queue(deadQueue);
	}

	@Bean("deadExchange")
	public DirectExchange deadExchange() {
		return new DirectExchange(deadExchange);
	}

	@Bean
	public Binding bindingExchangeMessage(Queue requestQueue, TopicExchange requestExchange) {
		Binding b = BindingBuilder.bind(requestQueue).to(requestExchange).with(binding);
		return b;
	}

	/**
	 * 死信队列与死信交换机绑定
	 * 
	 * @param queueMessage
	 * @param exchange
	 * @return
	 */
	@Bean
	public Binding bindingDeadExchangeMessage(Queue deadQueue, DirectExchange deadExchange) {
		Binding b = BindingBuilder.bind(deadQueue).to(deadExchange).with(deadbinding);
		return b;
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(new Jackson2JsonMessageConverter());
		return template;
	}

	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(new Jackson2JsonMessageConverter());
//		factory.setPrefetchCount(80);
//		factory.setConcurrentConsumers(2);
		return factory;
	}

	// @Bean
	// public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
	// return new RabbitAdmin(connectionFactory);
	// }

}
