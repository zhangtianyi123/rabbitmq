package zty.practise.springrabbit.ack;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import zty.practise.springrabbit.model.RequestEntity;
import zty.practise.springrabbit.model.ResponseEntity;

public class MessageManualRetryHandler implements ChannelAwareMessageListener {

		@Autowired
		private AutoNoCatchService chooseOneService;
		
		public void onMessage(Message message, Channel channel) throws Exception {
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
				ack = true;
				e.printStackTrace();
			}
			
			if(ack) {
				//消息重新入队
				boolean requeue = true;
				channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, requeue);
			} else {
				//手动在处理完以后发送ack
				channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
			}
			
		}
}
