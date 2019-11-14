package zty.practise.cloudrabbit.service;

import java.util.Random;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zty.practise.cloudrabbit.model.AlarmMessage;

/**
 * 模拟第三方的发送方
 * 
 * @author zhangtianyi
 *
 */
@Service("produceService")
@Slf4j
public class ProduceService {

	@Autowired
	private AmqpTemplate rabbitTemplate;
	
	private static final String[] data = new String[] { "aaa", "bbb", "ccc", "ddd", "eee", "fff", "ggg"};
	
	/**
	 * 自动模拟第三方定时发送，不发送时注释
	 */
	@Scheduled(fixedRate = 3000)
	public void send() {
		String exchange = "businessAdviceDestination";
		int instanceCount = 4;
		
		String key = data[new Random().nextInt(data.length)];
		AlarmMessage alarmMessage = new AlarmMessage();
		alarmMessage.setAlarmItemCode(key);
		alarmMessage.setAlarmMessageIdentifier(1L);
		
		System.out.println("Sending: " + key + " = "+ key.hashCode() + "=" + getRoutingKeyByHash(exchange, instanceCount, key));
		this.rabbitTemplate.convertAndSend(exchange, getRoutingKeyByHash(exchange, instanceCount, key), alarmMessage);
	}
	
	/**
	 * 普通的哈希算法确定分区，默认exchange-0
	 * @param exchange
	 * @param instanceCount
	 * @param key
	 * @return
	 */
	private String getRoutingKeyByHash(String exchange, int instanceCount, String key) { 
		String routingKey = exchange;
		
		int mod = 0;
		if(key.hashCode() % instanceCount >= 0 && key.hashCode() % instanceCount < instanceCount) {
			mod = key.hashCode() % instanceCount;
		}
		routingKey = routingKey + "-" + mod;
		
		return routingKey;
	}
}
