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

> bootrabbit项目

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
 
### 手动设置重试进入死信队列
 
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
 
### 本地缓存（prefetch）与并发消费（concurrency）

prefetch允许为每个consumer指定最大的unacked messages数目。简单来说就是用来指定一个consumer一次可以从Rabbit中获取多少条message并缓存在client中。一旦缓冲区满了，Rabbit将会停止投递新的message到该consumer中直到它发出ack。
(consumer线程内部维护了一个阻塞队列BlockingQueue——**经典的双重生产者消费者模式**

> 所以这个prefetch的大小实际上就是queuesize的大小

- prefetch可能会丢失消息，缓存在本地而为ack的消息可能因为服务挂掉而丢失

如果此时消费者线程数为2：
每个consumer每次会从queue中预抓取 10 条消息到本地缓存着等待消费。同时该channel的unacked数变为20。而Rabbit投递的顺序是，先为consumer1投递满10个message，再往consumer2投递10个message。下一条消息需要两个消费者中的一个ack至少一条之后才能继续投放。

> uncak = prefetch * consumernum = prefetch * (concurrency * psnum)


### 情形1：prefetch=n,concurreny=1

- 配置
 - spring.rabbitmq.listener.simple.prefetch = 100
 - spring.rabbitmq.listener.simple.prefetch = 1
 - AmqpSender类中使用send()方法发送正常消息
 - 调快AmqpSender-@Scheduled的发送速率
 - 使用线程休眠降低消费速度

 测试结果：消息顺序消费
 
### 情形2：prefetch=1,concurreny=n

- 配置
 - spring.rabbitmq.listener.simple.prefetch = 1
 - spring.rabbitmq.listener.simple.prefetch = 100
 - AmqpSender类中使用send()方法发送正常消息
 - 调快AmqpSender-@Scheduled的发送速率
 - 使用线程休眠降低消费速度

```
//模拟处理时间
Thread.sleep(RandomUtils.nextLong(500, 5000));
```

配置：
```
prefetch: 1
concurrency: 100
```

测试结果：消息被无序消费

### 情形3：prefetch=n,concurreny=n
- 配置
 - spring.rabbitmq.listener.simple.prefetch = 100
 - spring.rabbitmq.listener.simple.prefetch = 100
 - AmqpSender类中使用send()方法发送正常消息
 - 调快AmqpSender-@Scheduled的发送速率
 - 使用线程休眠降低消费速度

测试结果：消息被无序消费

> prefetch:The higher this is the faster the messages can be delivered, but the higher the risk of non-sequential processing
虽然一般情况下能保证preftch下的顺序消费，但是值越大，出现非顺序性消费的可能性就越高
 
 
 container启动的时候会根据设置的concurrency的值（同时不超过最大值）创建n个BlockingQueueConsumer
 
 BlockingQueueConsumer内部应该维护了一个阻塞队列BlockingQueue，prefetch应该是这个阻塞队列的长度，BlockingQueueConsumer内部有个queue，这个queue不是对应RabbitMQ的队列，而是Consumer自己维护的内存级别的队列，用来暂时存储从RabbitMQ中取出来的消息
 
> 对消息的顺序有苛刻要求的场景不适合并发消费

### 消息重复消费

幂等性

- 数据库去重表方式
- 内存redis方式


### Spring Cloud Stream(Rabbit)

定义绑定接口

```
public interface BusinessAdviceStreamClient {

	String INPUT = "inputBusinessAdvice";

    String OUTPUT = "outputBusinessAdvice";

    @Input(BusinessAdviceStreamClient.INPUT)
    SubscribableChannel input();

    @Output(BusinessAdviceStreamClient.OUTPUT)
    MessageChannel output();
}
```

生产者
```
@Component
@EnableBinding(value = { BusinessAdviceStreamClient.class })
public class SreamSender {

	@Autowired
	private BusinessAdviceStreamClient businessAdviceStreamClient;

	/**
	 * 发送业务通知
	 *
	 * @param alarmMessage
	 */
	public void sendAlarmMessage(Object alarmMessage) {
		boolean b = businessAdviceStreamClient.output().send(MessageBuilder.withPayload(alarmMessage).build());
	}
	
}
```

消费者
```
	@StreamListener(BusinessAdviceStreamClient.INPUT)
	public void process(AlarmMessage alarmMessage) {
		log.info("receive business message : {}", alarmMessage);
		
		//模拟业务处理（可能出现异常）
		alarmMessage.getAlarmItemCode().charAt(0);
	}
```

- 自定义错误处理：特定通道

```
// BusinessAdviceReceiver.java
@ServiceActivator(inputChannel = "businessAdviceDestination.businessAdviceGroup.errors") 
public void error(Message<?> message) {
    System.out.println("businessAdviceGroup Handling ERROR: " + message);
}
```

- 自定义错误处理：全局捕获

```
// BusinessAdviceReceiver.java
@StreamListener("errorChannel")
public void allError(Message<?> message) {
	System.out.println("all Handling ERROR: " + message);
}
```

配置文件
```
server:
  port: 8012
  
spring:
  cloud:
    stream:
      binders:
        litteRabbit:
          environment:
            spring:
              rabbitmq:
                addresses: 127.0.0.1:5672
                username: zhangtianyi
                password: zhangtianyi
                virtual-host: vhost_test01
          type: rabbit
      bindings:
        outputBusinessAdvice:
          binder: litteRabbit
          destination: businessAdviceDestination
        inputBusinessAdvice:
          binder: litteRabbit
          destination: businessAdviceDestination
          group: businessAdviceGroup
          consumer:
            # 最多尝试处理几次，默认3	
            maxAttempts: 5
      rabbit:
        bindings:
          inputBusinessAdvice:
            consumer:
              auto-bind-dlq: true
              republish-to-dlq: true
              # 对顺序不做要求时
              prefetch: 250
              maxConcurrency: 5
```

死信队列：
```
# 开启死信队列（重试后，进入死信队列）
auto-bind-dlq: true
# 附带传递错误信息到死信队列
republish-to-dlq: true
```

修改错误时重试次数
```
# 最多尝试处理几次，默认3	
maxAttempts: 5
```

> 重试+死信队列为较好的处理方式，直接丢弃往往都不能接受，而requeue重新排队很多时候错误都是非瞬时的，可能会陷入死循环，而网络抖动等问题，可以通过重试解决。而自定义的降级策略往往很难取得良好的效果。


定时发送
```
@Bean
@InboundChannelAdapter(value = BusinessAdviceStreamClient.OUTPUT,	
        poller = @Poller(fixedDelay = "1000", maxMessagesPerPoll = "1"))	
public MessageSource<Object> sendAlarmMessageSchedule() {	
	AlarmMessage alarmMessage = new AlarmMessage();
	alarmMessage.setAlarmItemCode("code");
	alarmMessage.setAlarmMessageIdentifier(1L);
	log.info("send schedule message");
    return () -> new GenericMessage<>(alarmMessage);	
}	
```


### 分区

单纯消费者组（spring cloud stream group） + RabbitMQ的时候  ，虽然每个消息只发到其中的一个消费者实例，但是并不是同一个队列的消息每次发到同一个消费者。而且比如一个队列3个消费者，并不是互相等ack,第一个消费者没有ack ，后面的消息还是可以发给其它消费者，违背了队列有序性。

在消费组中我们可以保证消息不会被重复消费，但是在同组下有多个实例的时候，我们无法确定每次处理消息的是不是被同一消费者消费，分区的作用就是为了确保具有共同特征标识的数据由同一个消费者实例进行处理

RabbitMQ 想要保证顺序性，需要队列和消费者一一对应，但是这种绑定关系的配置使得消费者程序难于水平扩展，所以结合spring cloud stream 提供的分区（借鉴kafka）机制，可以实现自动的rebalance

RabbitMQ 天然是不支持分区的，但是spring cloud stream 提供一种一致性的方式，使得**同种标识的数据能够被同一个消费者实例处理**

消费者配置：
```
consumer:
    partitioned: true
    # 分区号
    instance-index: 3
```

生产者配置
```
producer:
    #分区表达式, 例如当表达式的值为1, 那么在订阅者的instance-index中为1的接收方, 将会执行该消息
    partition-key-expression: headers['partitionKey']
    #指定参与消息分区的消费端节点数量
    partition-count: 4
```

生产者发送消息
```
//TimerSource.java
	@InboundChannelAdapter(channel = BusinessAdviceStreamClient.OUTPUT, poller = @Poller(fixedRate = "2000"))
	public Message<?> generate() {
		String value = data[new Random().nextInt(data.length)];
		AlarmMessage alarmMessage = new AlarmMessage();
		alarmMessage.setAlarmItemCode(value);
		alarmMessage.setAlarmMessageIdentifier(1L);
		System.out.println("Sending: " + value + " = "+ value.hashCode());
		return MessageBuilder.withPayload(alarmMessage).setHeader("partitionKey", value).build();
	
```

spring cloud stream 默认会取key值的hashcode()值对instancecount取余，并映射到(routingkey)对应的queue

为了分区生产者和使用者，队列以分区索引作为后缀，并使用分区索引作为路由键（routing key）

虽然spring cloud stream基于boot提供了自动配置分区的逻辑，但是对于第三方的任务不一定使用spring cloud stream作为生产者来发送消息

所以这里基于cloud stream的原理用普通哈希算法实现分区选择

```
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
```


- 消息确认（ACK机制）

spring cloud stream 默认为自动确认（AUTO）即不报错就返回ack。出于可靠性的角度，应该使用手动确认（MANUAL），在业务处理成功之后再ACK

配置：
```
spring.cloud.stream.rabbit.bindings.input.consumer.acknowledge-mode=manual
```

消费者代码：
```
// 如果配置了手动确认，代码里没有手动ack的逻辑，消息将永远得不到ack,阻塞队列
	@StreamListener(BusinessAdviceStreamClient.INPUT)
	public void process(AlarmMessage alarmMessage, @Header(AmqpHeaders.CHANNEL) Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag) {
		log.info("consumer-1 receive business message : {}", alarmMessage.getAlarmItemCode());
		
		//模拟业务处理（可能出现异常）
		alarmMessage.getAlarmItemCode().charAt(0);
		
		//手动确认
		try {
			channel.basicAck(deliveryTag, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
```


  [1]: http://static.zybuluo.com/zhangtianyi/u3v6v65fq2z4bk7ml38wdc5o/image_1dltqpsj61g961i6v1hb26edmj39.png




