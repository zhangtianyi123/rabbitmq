package zty.practise.cloudrabbit.consume;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import lombok.extern.slf4j.Slf4j;
import zty.practise.cloudrabbit.model.AlarmMessage;
import zty.practise.cloudrabbit.binder.BusinessAdviceStreamClient;

import java.io.IOException;
import java.util.Map;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateAcknowledgeAmqpException;
import org.springframework.amqp.support.AmqpHeaders;

@Component
@Slf4j
@EnableBinding(BusinessAdviceStreamClient.class)
public class BusinessAdviceReceiver {

	@Value("${spring.cloud.stream.bindings.inputBusinessAdvice.group}")
	private String businessAdviceGroup;

	@Value("${spring.cloud.stream.bindings.inputBusinessAdvice.consumer.maxAttempts}")
	private Long maxAttempts;
	
	/**
	 * 消费业务通知消息
	 * 
	 * @param alarmMessage
	 */
	@StreamListener(BusinessAdviceStreamClient.INPUT)
	public void process(AlarmMessage alarmMessage, @Header(AmqpHeaders.CHANNEL) Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag, @Header("deliveryAttempt") Long deliveryAttempt) {
		// throw new AmqpRejectAndDontRequeueException("failed");
		// 不发送到死信队列，直接放弃
		// throw new ImmediateAcknowledgeAmqpException("Failed after 4
		// attempts");

		log.info("consumer-4 receive business message : {}, deliveryAttempt={}", alarmMessage.getAlarmItemCode(),
				deliveryAttempt);

		if (deliveryAttempt < maxAttempts) {
			// 模拟业务处理（可能出现异常）
			alarmMessage.getAlarmItemCode().charAt(0);
			
			// 手动确认
			try {
				channel.basicAck(deliveryTag, false);
				log.info("business message : {} handle success",  alarmMessage.getAlarmItemCode());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// 手动拒绝，进入死信队列
			try {
				channel.basicNack(deliveryTag, false, false);
				log.info("business message : {} send to dlq",  alarmMessage.getAlarmItemCode());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

	}

	/**
	 * 自定义的特定通道错误处理,降级逻辑 process 方法已成将会发送错误到此订阅者
	 * 
	 * @param message
	 */
	// @ServiceActivator(inputChannel =
	// "businessAdviceDestination.businessAdviceGroup.errors")
	// public void error(Message<?> message) {
	// System.out.println("businessAdviceGroup Handling ERROR: " + message);
	// }

	/**
	 * 自定义的全局错误处理,降级逻辑 全局错误处理
	 * 
	 * @param message
	 */
	// @StreamListener("errorChannel")
	// public void allError(Message<?> message) {
	// System.out.println("all Handling ERROR: " + message);
	// }
	//

}
