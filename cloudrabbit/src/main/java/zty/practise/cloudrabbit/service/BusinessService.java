package zty.practise.cloudrabbit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import zty.practise.cloudrabbit.model.AlarmMessage;
import zty.practise.cloudrabbit.util.SreamSender;


@Slf4j
@Transactional
@Service
public class BusinessService {

	@Autowired
	private SreamSender sreamSender;
	
	public void handleAndSendMessage() {
		AlarmMessage alarmMessage = new AlarmMessage();
		alarmMessage.setAlarmItemCode("code");
		alarmMessage.setAlarmMessageIdentifier(1L);
		sreamSender.sendAlarmMessage(alarmMessage);
		log.info("send message");
	}
	
	public void handleAndSendErrorMessage() {
		AlarmMessage alarmMessage = new AlarmMessage();
		alarmMessage.setAlarmItemCode(null);
		alarmMessage.setAlarmMessageIdentifier(1L);
		sreamSender.sendAlarmMessage(alarmMessage);
		log.info("send message");
	}
	
	public void handleAndSendPartitionMessage() {
		AlarmMessage alarmMessage = new AlarmMessage();
		alarmMessage.setAlarmItemCode("code");
		alarmMessage.setAlarmMessageIdentifier(1L);
		sreamSender.sendPartitionAlarmMessage(alarmMessage);
		log.info("send message");
	}
	
	public void handleAndSendPartitionErrorMessage() {
		AlarmMessage alarmMessage = new AlarmMessage();
		alarmMessage.setAlarmItemCode(null);
		alarmMessage.setAlarmMessageIdentifier(1L);
		sreamSender.sendPartitionAlarmMessage(alarmMessage);
		log.info("send message");
	}
	
	public void handleAndSendPartitionErrorMessageTo0() {
		AlarmMessage alarmMessage = new AlarmMessage();
		alarmMessage.setAlarmItemCode(null);
		alarmMessage.setAlarmMessageIdentifier(1L);
		sreamSender.sendPartitionAlarmMessageTo0(alarmMessage);
		log.info("send message");
	}
}
