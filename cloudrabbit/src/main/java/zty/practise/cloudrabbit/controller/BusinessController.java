package zty.practise.cloudrabbit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import zty.practise.cloudrabbit.service.BusinessService;

@RestController
public class BusinessController {

	@Autowired
	private BusinessService businessService;
	
	@PostMapping("/send")
	@ApiOperation("发送正常的消息。需要在未设置分区时使用，否则报错")
	public void send() {
		businessService.handleAndSendMessage();
	}
	
	@PostMapping("/send/error")
	@ApiOperation("发送异常的消息。需要在未设置分区时使用，否则报错")
	public void sendError() {
		businessService.handleAndSendErrorMessage();
	}
	
	@PostMapping("/partition/send")
	@ApiOperation("发送正常的消息。需要在设置分区时使用")
	public void sendPartition() {
		businessService.handleAndSendPartitionMessage();
	}
	
	@PostMapping("/partition/send/error")
	@ApiOperation("发送正常的消息。需要在设置分区时使用")
	public void sendErrorPartition() {
		businessService.handleAndSendPartitionErrorMessage();
	}
	
	@PostMapping("/partition/send/0/error")
	@ApiOperation("发送正常的消息。需要在设置分区时使用")
	public void sendErrorPartitionTo0() {
		businessService.handleAndSendPartitionErrorMessageTo0();
	}
}
