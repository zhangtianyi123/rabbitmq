# RabbitMQ


---

## 消息丢失问题

验证消息丢失的一种方法：利用消息队列的有序性，在生产端，每个发出的消息附加一个连续递增的序号，然后再消费端验证序号的连续性 （前提是不破坏有序性）

- 生产阶段的请求确认机制
内存收到即确认/持久化磁盘后确认/将消息发到两个以上集群节点后确认
publisher-confirms：消息发送到Exchange后触发回调
publisher-returns：消息从Exchange发送到queue失败时触发回调

- 存储阶段的持久化机制
 - 将queue的持久化标识durable设置为true,则代表是一个持久的队列
 - 发送消息的时候将deliveryMode=2

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

但是如果把ack的代码移动到onMessage方法的首行，也会报错，但是消息会直接消费掉
```
	public void onMessage(Message message, Channel channel) throws Exception {
		channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
		//调用业务方法
	}
```


手动消息可以灵活的发送ack，nack(重新入队)，丢弃消息
```
//手动在处理完以后发送ack
channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
		
//消息重新入队
boolean requeue = true;
channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, requeue);
		
//拒绝丢弃消息
boolean reject = false;
channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, reject);
```

> 需要注意的是不管是ack，nack ，发生异常的时候代码都无法执行到，所以需要与try-catch搭配

### 死信队列

RabbitMQ的TTL 全称为Time-To-Live 表示消息的有效期，消息如果在队列中一直没有被消费并且存在时间超过了TTL 消息就会编程死信（Dead Message）,后续无法再被消费了

- 设置TTL的两种方式
 - 声明队列的时候设置，对整个队列的消息都有效（x-message-ttl）
 - 发送消息的时候设置属性，每个消息不同

两种设置取最小值为准

DXL:Dead-Letter-Exchange 死信交换机

- 消息变为死信的情况
 - 消息被拒绝（Basic.Reject/Basix.Nack）并且设置requeue参数为false
 - 消息过期
 - 队列达到最大的长度

当消息在一个队列中变成死信队列之后，可以自动发送到设置的DLX,进而被路由到DLX绑定的死信队列

> 可以利用这个机制实现**延时队列**（原队列没有消费者，过期后自动发送到死信队列）

配置死信交换机和死信路由并配置死信队列
```
@Bean("requestQueue")
public Queue requestQueue() {
	Map<String, Object> args = new HashMap<>(2);
	args.put("x-dead-letter-exchange", deadExchange);
	args.put("x-dead-letter-routing-key", deadbinding);
	return QueueBuilder.durable(requestQueue).withArguments(args).build();
}
```

```
public Binding bindingDeadExchangeMessage(Queue deadQueue, DirectExchange deadExchange) {
	Binding b =  BindingBuilder.bind(deadQueue).to(deadExchange).with(deadbinding);
	return b;
}
```

> 使用构建者模式来创建queue Exchange... 参数属性通过Map传递（比如死信队列）
如果声明的属性和broker实体已经建立的不一样，那么启动会报错

当nack时（没有requeue时，将会把消息传入死信队列中）

```
if(ack) {
	//消息重新入队
	boolean requeue = true;
	channel.basicNack(tag, false, false);
	log.info("重新入队");
} else {
	//手动在处理完以后发送ack
	channel.basicAck(tag, false);
	log.info("手动确认");
}
```

消费死信队列就能处理这些脏数据

![image_1dltqpsj61g961i6v1hb26edmj39.png-33.3kB][1]
 
 手动模式下通过代码层面的缓存机制实现，在异常处理的情况下，如果异常超过阈值则丢弃（进而会被放入死信队列），如果异常还不满阈值，则重试（requeue）;如果超过阈值，比如三五次都还处理失败，基本可以证明是脏数据
 
```
 //调用业务方法
boolean ack = false;
String reqId = requestEntity.getReqId();;
try {
	ResponseEntity response = chooseOneService.doPressureTest(requestEntity);
} catch(Exception e) {
	log.info("exception");
	ack = true;
	e.printStackTrace();
}
		
if(ack) {
	//消息重新入队
    if(isMaxAttempt(reqId)) {
    	boolean reject = false;
    	channel.basicNack(tag, false, reject);
    	log.info("丢弃死信:{}", reqId);
    } else {
    	boolean requeue = true;
    	channel.basicNack(tag, false, requeue);
    	log.info("重新入队:{}", reqId);
    }
} else {
	//手动在处理完以后发送ack
	channel.basicAck(tag, false);
	log.info("确认成功:{}", reqId);
}
```

```
/**
* 设置异常前提下的重试次数，手动模式下尝试一定的次数，失败放入死信
*/
private boolean isMaxAttempt(String reqId) {
	MessageIdCache.cache.put(reqId, MessageIdCache.cache.getOrDefault(reqId, 0) + 1);
	if(MessageIdCache.cache.get(reqId) > 3) {
		return true;
	}
	return false;
}
```
 




  [1]: http://static.zybuluo.com/zhangtianyi/u3v6v65fq2z4bk7ml38wdc5o/image_1dltqpsj61g961i6v1hb26edmj39.png