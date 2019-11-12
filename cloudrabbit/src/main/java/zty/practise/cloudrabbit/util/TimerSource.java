package zty.practise.cloudrabbit.util;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.support.GenericMessage;

import lombok.extern.slf4j.Slf4j;
import zty.practise.cloudrabbit.binder.BusinessAdviceStreamClient;
import zty.practise.cloudrabbit.model.AlarmMessage;

@Slf4j
@EnableBinding(value = { BusinessAdviceStreamClient.class })
public class TimerSource {

	/**
	 * 定时发布消息
	 * @return
	 */
//	@Bean
//    @InboundChannelAdapter(value = BusinessAdviceStreamClient.OUTPUT,	
//            poller = @Poller(fixedDelay = "1000", maxMessagesPerPoll = "1"))	
//    public MessageSource<Object> sendAlarmMessageSchedule() {	
//		AlarmMessage alarmMessage = new AlarmMessage();
//		alarmMessage.setAlarmItemCode("code");
//		alarmMessage.setAlarmMessageIdentifier(1L);
//		log.info("send schedule message");
//        return () -> new GenericMessage<>(alarmMessage);	
//    }	
}
