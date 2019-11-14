package zty.practise.cloudrabbit.util;

import java.util.Date;
import java.util.Random;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

import lombok.extern.slf4j.Slf4j;
import zty.practise.cloudrabbit.binder.BusinessAdviceStreamClient;
import zty.practise.cloudrabbit.model.AlarmMessage;

@Slf4j
@EnableBinding(value = { BusinessAdviceStreamClient.class })
public class TimerSource {

	int count = 0;

	/**
	 * 定时发布消息
	 * 
	 * @return
	 */
//	@Bean
//	@InboundChannelAdapter(value = BusinessAdviceStreamClient.OUTPUT, poller = @Poller(fixedDelay = "2000", maxMessagesPerPoll = "1"))
//	public MessageSource<Object> sendAlarmMessageSchedule() {
//		AlarmMessage alarmMessage = new AlarmMessage();
//		alarmMessage.setAlarmItemCode(count++ + "-" + new Date());
//		alarmMessage.setAlarmMessageIdentifier(1L);
//		log.info("send schedule message");
//		return () -> new GenericMessage<>(alarmMessage);
//	}

	private static final String[] data = new String[] { "aaa", "bbb", "ccc", "ddd", "eee", "fff", "ggg"};

//	@InboundChannelAdapter(channel = BusinessAdviceStreamClient.OUTPUT, poller = @Poller(fixedRate = "2000"))
//	public Message<?> generate() {
//		String value = data[new Random().nextInt(data.length)];
//		AlarmMessage alarmMessage = new AlarmMessage();
//		alarmMessage.setAlarmItemCode(value);
//		alarmMessage.setAlarmMessageIdentifier(1L);
//		System.out.println("Sending: " + value + " = "+ value.hashCode());
//		return MessageBuilder.withPayload(alarmMessage).setHeader("partitionKey", value).build();
//	}
}
