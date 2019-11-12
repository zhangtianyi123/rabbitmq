package zty.practise.cloudrabbit.consume;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import zty.practise.cloudrabbit.model.AlarmMessage;
import zty.practise.cloudrabbit.binder.BusinessAdviceStreamClient;

@Component
@Slf4j
@EnableBinding(BusinessAdviceStreamClient.class)
public class BusinessAdviceReceiver {

	@Value("${spring.cloud.stream.bindings.inputBusinessAdvice.group}")
	private String businessAdviceGroup;
	
	/**
	 * 消费业务通知消息
	 * @param alarmMessage
	 */
	@StreamListener(BusinessAdviceStreamClient.INPUT)
	public void process(AlarmMessage alarmMessage) {
		log.info("receive business message : {}", alarmMessage);
		
		//模拟业务处理（可能出现异常）
		alarmMessage.getAlarmItemCode().charAt(0);
	}
	
	/**
	 * 自定义的特定通道错误处理,降级逻辑
	 * process 方法已成将会发送错误到此订阅者
	 * @param message
	 */
//	@ServiceActivator(inputChannel = "businessAdviceDestination.businessAdviceGroup.errors") 
//	public void error(Message<?> message) {
//		System.out.println("businessAdviceGroup Handling ERROR: " + message);
//	}
	
	/** 
	 * 自定义的全局错误处理,降级逻辑
	 * 全局错误处理
	 * @param message
	 */
//	@StreamListener("errorChannel")
//	public void allError(Message<?> message) {
//		System.out.println("all Handling ERROR: " + message);
//	}
//	
	
}
