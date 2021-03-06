package zty.practise.bootrabbit.consume;

import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import lombok.extern.slf4j.Slf4j;
import zty.practise.bootrabbit.model.RequestEntity;
import zty.practise.bootrabbit.model.ResponseEntity;
import zty.practise.bootrabbit.util.MessageIdCache;

@Service("amqpReceiver")
@Slf4j
public class AmqpReceiver {

	@Autowired
	private PressureTestService chooseOneService;

	@RabbitListener(queues = "requestQueue")
	public void process(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag)
			throws ParseException, IOException, InterruptedException {
		// log.info("Receiver:{}", message);

		String body = "";

		// 消息转换和处理，如果以下逻辑失败，是典型的脏数据场景
		try {
			body = new String(message.getBody(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		ObjectMapper mapper = new ObjectMapper();
		RequestEntity requestEntity = null;
		try {
			requestEntity = mapper.readValue(body, RequestEntity.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//模拟处理时间
		Thread.sleep(RandomUtils.nextLong(500, 5000));

		// 调用业务方法
		boolean ack = false;
		String reqId = requestEntity.getReqId();
		try {
			ResponseEntity response = chooseOneService.doPressureTest(requestEntity);
		} catch (Exception e) {
			ack = true;
			e.printStackTrace();
		}

		if (ack) {
			// 消息重新入队
			if (isMaxAttempt(reqId)) {
				boolean reject = false;
				channel.basicNack(tag, false, reject);
				log.info("丢弃死信:{}", reqId);
			} else {
				boolean requeue = true;
				channel.basicNack(tag, false, requeue);
				log.info("重新入队:{}", reqId);
			}
		} else {
			// 手动在处理完以后发送ack
			channel.basicAck(tag, false);
			log.info("{}线程确认成功:{}", Thread.currentThread(), reqId);
		}
	}

	/**
	 * 设置异常前提下的重试次数，手动模式下尝试一定的次数，失败放入死信
	 * 
	 * @param reqId
	 * @return
	 */
	private boolean isMaxAttempt(String reqId) {
		MessageIdCache.cache.put(reqId, MessageIdCache.cache.getOrDefault(reqId, 0) + 1);
		if (MessageIdCache.cache.get(reqId) > 3) {
			return true;
		}
		return false;
	}

}
