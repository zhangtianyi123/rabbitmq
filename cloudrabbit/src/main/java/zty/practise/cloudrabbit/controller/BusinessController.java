package zty.practise.cloudrabbit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import zty.practise.cloudrabbit.service.BusinessService;

@RestController
public class BusinessController {

	@Autowired
	private BusinessService businessService;
	
	@PostMapping("/send")
	public void send() {
		businessService.handleAndSendMessage();
	}
}
