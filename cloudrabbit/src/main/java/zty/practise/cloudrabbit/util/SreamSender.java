package zty.practise.cloudrabbit.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.integration.annotation.Poller;

import zty.practise.cloudrabbit.binder.BusinessAdviceStreamClient;

/**
 * Spring Cloud Stream 发送器
 * 
 * 使用特定Binder时需要在此声明注入
 * 
 * @EnableBinding声明多个绑定用逗号隔开
 * 
 * @author zhangtianyi
 *
 */
@Component
@EnableBinding(value = { BusinessAdviceStreamClient.class })
public class SreamSender {

	@Autowired
	private BusinessAdviceStreamClient businessAdviceStreamClient;

	/**
	 * 发送业务通知
	 *
	 * @param alarmMessage
	 */
	public void sendAlarmMessage(Object alarmMessage) {
		boolean b = businessAdviceStreamClient.output().send(MessageBuilder.withPayload(alarmMessage).build());
	}
	
}
