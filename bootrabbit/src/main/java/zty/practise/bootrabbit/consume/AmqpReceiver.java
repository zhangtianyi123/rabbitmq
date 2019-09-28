package zty.practise.bootrabbit.consume;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
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

@Service("amqpReceiver")
@Slf4j
public class AmqpReceiver {
	
	@Autowired
	private PressureTestService chooseOneService;

	@RabbitListener(queues = "requestQueue", containerFactory="rabbitListenerContainerFactory")
	public void process(Message message, Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long tag) throws ParseException, IOException {
//		log.info("Receiver:{}", message);
		
		String body = "";
		
		//消息转换和处理，如果以下逻辑失败，是典型的脏数据场景
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
		
		//调用业务方法
		boolean ack = false;
		try {
			ResponseEntity response = chooseOneService.doPressureTest(requestEntity);
		} catch(Exception e) {
			log.info("exception");
			ack = true;
			e.printStackTrace();
		}
		
		if(ack) {
			//消息重新入队
			boolean requeue = true;
			channel.basicNack(tag, false, requeue);
			log.info("重新入队");
		} else {
			//手动在处理完以后发送ack
			channel.basicAck(tag, false);
			log.info("手动确认");
		}
	}

}
