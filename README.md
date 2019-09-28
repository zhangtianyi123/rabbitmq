# RabbitMQ

---

## 消息丢失问题

验证消息丢失的一种方法：利用消息队列的有序性，在生产端，每个发出的消息附加一个连续递增的序号，然后再消费端验证序号的连续性 （前提是不破坏有序性）

- 生产阶段的请求确认机制
内存收到即确认/持久化磁盘后确认/将消息发到两个以上集群节点后确认
publisher-confirms：消息发送到Exchange后触发回调
publisher-returns：消息从Exchange发送到queue失败时触发回调


- 消费阶段的ack机制
 - none:收到消息即立即确认，如果后续消费端异常没能成功处理消息，broker也不会重发，相当于不确认
 - auto:默认未发异常时ack
 - manual:手动确认ack, ack、nack、reject等方式，可以重新入队重发，也可以直接拒绝丢弃消息

> 手动确认的重新入队应该是针对网络等问题导致的偶然失败，而非数据格式问题，后者无论处理多少次都会失败，进入死循环堵死队列，可以根据异常类型选择冲入队列或是拒绝丢弃，可以重发消息到队列尾部不阻塞，也可以通过死信队列的方式来处理

```
// 通过finally块来保证Ack/Nack会且只会执行一次
if (action == Action.ACCEPT) {
    channel.basicAck(tag, true);
// 重试
} else if (action == Action.RETRY) {
    channel.basicNack(tag, false, true);
    Thread.sleep(2000L);
// 拒绝消息也相当于主动删除mq队列的消息
} else {
    channel.basicNack(tag, false, false);
}
/*channel.basicNack与 channel.basicReject的区别在于basicNack可以拒绝多条消息，而basicReject一次只能拒绝一条消息*/
```

<br />
### auto模式下-tryCatch处理-脏数据

- 配置启用
 - ack包的启动类配置文件指向ack/amqp-consume-autoack
 - ack包监听类MessageHandler装配的服务为AutoAndCatchService

当生产者发出脏数据时（lotName=null）,消费者try-catch了业务代码，那么即时针对空指针的操作，也不会抛出NPE,在消费端的业务执行是失败的，但是auto模式并bucare,由于catch不会抛出异常，那么也就能正常ack.

```
@Service
public class AutoAndCatchService {
	public ResponseEntity doPressureTest(RequestEntity entity) {
		ResponseEntity retParams = new ResponseEntity();
		try {
			String lotName = entity.getLotName();
			
			//截取lotName的前缀，这个操作依赖于lotName数据的约定，如果为Empty的数据即为脏数据，会发生RTE
			String lotHead = lotName.substring(0, 2);
		} catch (Exception e) {
		}
		return retParams;
	}

}
```

<br/>
### auto模式下-无tryCatch处理-脏数据 

- 配置启用
 - ack包的启动类配置文件指向ack/amqp-consume-autoack
 - ack包监听类MessageHandler装配的服务为AutoNoCatchService

当生产者发出脏数据时（lotName=null），消费者没有try-catch业务代码，业务处理过程中由于空指针会抛出运行时异常，将会导致这一个脏数据一直无法被消费，阻塞队列

```
@Service
public class AutoNoCatchService {
	public ResponseEntity doPressureTest(RequestEntity entity) {
		ResponseEntity retParams = new ResponseEntity();
		
		String lotName = entity.getLotName();
			
		//截取lotName的前缀，这个操作依赖于lotName数据的约定，如果为Empty的数据即为脏数据，会发生RTE
		String lotHead = lotName.substring(0, 2);
		
		return retParams;
	}

}
```

### none模式下-无tryCatch处理-脏数据  
- 配置启用
 - ack包的启动类配置文件指向ack/amqp-consume-noneack
 - ack包监听类MessageHandler装配的服务为AutoNoCatchService

虽然未捕获异常，脏数据又会导致导致空指针异常，但是none模式下，相当于放弃了ack机制，不会阻塞队列，但是也会丢失消息
结果就是可以看到一路报错一路消费

```
	<rabbit:listener-container
		connection-factory="connectionFactory" acknowledge="none" message-converter="jsonMessageConverter">
		<rabbit:listener ref="messageHandler" method="handleMessage"
			queue-names="${requestQueue}" />
	</rabbit:listener-container>
```

### manual模式下-无tryCatch处理-处理后手动ack-脏数据

- 配置启用
 - ack包的启动类配置文件指向ack/amqp-consume-manualack
 - ack包监听类MessageManualHandler装配的服务为AutoNoCatchService

手动ack，可以灵活的确定执行的策略，ack的位置。因为在ack之前发生了空指针异常，脏数据也无法被ack

```
public class MessageManualHandler implements ChannelAwareMessageListener {

	@Autowired
	private AutoNoCatchService chooseOneService;
	
	public void onMessage(Message message, Channel channel) throws Exception {
		//调用业务方法
		
		//手动在处理完以后发送ack
		channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
	}

```
 




