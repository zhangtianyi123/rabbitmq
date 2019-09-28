package zty.practise.springrabbit.consume;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import zty.practise.springrabbit.model.RequestEntity;
import zty.practise.springrabbit.model.ResponseEntity;

@Component("messageHandler")
public class MessageHandler implements MessageListener {
	
	@Autowired
	private PressureTestService pressureTestService;

	public void onMessage(Message message) {
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
		ResponseEntity response = pressureTestService.doPressureTest(requestEntity);
	}
	
}
