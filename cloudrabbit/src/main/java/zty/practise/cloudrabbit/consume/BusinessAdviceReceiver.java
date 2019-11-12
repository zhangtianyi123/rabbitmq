package zty.practise.cloudrabbit.consume;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import zty.practise.cloudrabbit.model.AlarmMessage;
import zty.practise.cloudrabbit.binder.BusinessAdviceStreamClient;

@Component
@Slf4j
@EnableBinding(BusinessAdviceStreamClient.class)
public class BusinessAdviceReceiver {

	@StreamListener(BusinessAdviceStreamClient.INPUT)
	public void process(AlarmMessage alarmMessage) {
		log.info("receive business message : {}", alarmMessage);
	}
}
