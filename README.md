# Spring功能学习，测试，使用

# scope与spring并发请求

- scope介绍

scope描述对象在spring容器（IOC容器）中的生命周期，也可以理解为对象在spring容器中的创建方式。
1. **singleton**  此取值时表明容器中创建时只存在一个实例，所有引用此bean都是单一实例。controller,service,dao均默认单例
2. **prototype** 原型模式，会每次都重新生成一个新的对象给请求方（线程）
3. request 为每个HTTP请求创建一个全新的RequestPrecessor对象，当请求结束后，该对象的生命周期即告结束，如同java web中request的生命周期，可以看做prototype的一种特例
4. 如果java web中session的生命周期
5. global session 

- 经典的竞态条件，controller层测试并发的i++

基于默认spring为每个请求开一个线程，且controller是单例的，那么多个请求循环同时发到如下REST接口：
```
@RestController
public class SingleController {

	private int count = 0;
	
	@GetMapping("/test/single")
	public Integer send() throws InterruptedException{
		Thread.sleep(5000);
		count++;
		return count;
	}
}
```
[代码链接][1]

使用postman测试结果如下,证明了其单例和并发:

![image_1dj8c2doa40pks1fpblk15ff9.png-22.6kB][2]


- 使用原型scope：
```
@RestController
@Scope("prototype")
public class PrototypeController {

	private int count = 0;
	
	@GetMapping("/test/prototype")
	public Integer send() throws InterruptedException{
		Thread.sleep(5000);
		count++;
		return count;
	}
}
```

使用postman结果无论测试多少次，结果都是1,如果打印线程，也推断能每次打印不同的线程名。

- 测试单例并发下的竞态条件问题
对于内存堆变量有无数种手法解决其并发安全
对于数据库数据主要落实到数据库事务+数据库原子操作+乐观锁等机制
而对于缓存等中间件，redis单个操作原子，但是复合操作存在竞态条件，如下测试：

先后执行/getwait和/get 理论上只应该查询一次DB，但是由于存在竞态条件，所以查询两次
![image_1dj8f4map13nh1l7511h712ak7o016.png-69.2kB][3]

```
public String getGlobalDesc(String key) {
		if(StringUtils.isEmpty(key)) {
			return null;
		}
		
		String description = (String) cacheUtils.get(key);
		if(description != null) {
			return description;
		}
		
		description = getGlobalDescFromDB(key);
		cacheUtils.set(key, description);
		cacheUtils.expire(key, expireTime);
		
		return description;
	}
```
[代码链接][4]

高并发的版本
```
final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	final Lock readLock = rwLock.readLock();
	final Lock writeLock = rwLock.writeLock();

	public String getGlobalDescSync(String key) {
		if (StringUtils.isEmpty(key)) {
			return null;
		}

		String description = null;
		readLock.lock();
		try {
			description = (String) CacheUtils.get(key);
		} finally {
			readLock.unlock();
		}

		if (description != null) {
			return description;
		}

		writeLock.lock();
		try {
			//高并发情况下为了节省DBConnecition 在临界区内二次验证
			description = (String) CacheUtils.get(key);
			if(description == null) {
				description = getGlobalDescFromDB(key);
				CacheUtils.set(key, description);
				CacheUtils.expire(key, expireTime);
			}
		} finally {
			writeLock.unlock();
		}
		return description;
	}
```
[代码链接][5]

# Spring事务与安全
当前线程执行到事务控制的方法，AOP拦截之后，申请DBConnection等资源，开启事务，将资源存放到ThreadLocal变量中...事务执行完后提交释放资源清空线程本地存储

- 事务四大特性
 - 原子性
 - 一致性
 - 隔离性
 - 持久性

- 隔离级别
 - 脏读（读未提交）
 - 不可重复读（针对单行数据，更新）
 - 幻读（针对多行/表数据 增删）

![image_1djat9fa618dt10h4dj489k12qe19.png-61.2kB][6]

- 读未提交：读无锁/写共享锁，事务提交释放。 -> 写的时候（提交前）可能读到中间状态
- 读提交：读共享锁，**读完释放**/写独占锁，事务提交释放 -> 写的时候不能读了，但可能读完写再读
- **可重复读**：读共享锁，事务提交释放/写独占锁，事务提交释放 -> 读时（直到事务结束）不能写
- 串行化：读表级共享锁，事务提交释放/写表级独占锁，事务提交释放

- 对于无锁读和读锁读和写锁读
 - 当前读： select .. where ... 是无锁的
 - 快照读： select .. where ... lock in share mode 读锁
 - 写锁读： select .. where ... for update 写锁

### 证明事务提交

无锁读：select 查询单行数据
![image_1djb1p7h31cb5mmjbv614eg1mnq1m.png-173.2kB][7]

### 脏读测试（读未提交）

事务2 更新而不提交：
![image_1djb2tr30148it6gea81cs0tpg4o.png-43.8kB][8]

事务1 读：（读到的结果为原值而非dirtyData）
![image_1djb2sin11mv48in1hv0sh05t4b.png-13.1kB][9]

### 不可重复读测试
事务1 第一次无锁读：
![image_1djb42lnh1s081k7pqbl11u32pr9.png-53.3kB][10]

事务2：更新并提交
![image_1djb449241r7lkd77j1s701ngm.png-7.7kB][11]

事务1：第二次无锁读
在同一个事务中第二次读，结果与第一次相同
![image_1djb48nhv16fj56al97hjp1euo13.png-1.7kB][12]

### 幻读测试
事务1： 第一次无锁读表，size=1
![image_1djb59u6ufur18eg1fquoal12sp3i.png-56.9kB][13]

事务2：插入一条数据
![image_1djbaoura1l011aqalkc1rdl33v.png-12.7kB][14]

事务1： 第二次无锁读表，size仍然为1

### 第一类丢失更新

> 更新分两种，一种是直接覆盖更新，一种是依赖于查询结果的更新，后者存在竞态条件，比如i++, set(get() + 1)

事务1：开始事务
![image_1djbcv0ot12e01lphib0i71cac4s.png-20.8kB][15]

事务2：更新并提交事务
![image_1djbd0lag1o3h1kam1up09u5edt59.png-51.3kB][16]

事务1：回滚（未覆盖更新值）
![image_1djbd3qvjtad1t61lt01s43126i5m.png-32.9kB][17]

### 第二类丢失更新

事务1：查询原始数据（select）
![image_1djbdv9hm1ojj3phse81nkb1rl6j.png-21kB][18]

事务2：基于数据更新
![image_1djbe1ob8opor01jkc28l1nep70.png-49.4kB][19]

事务1：继续数据更新(update 与之前的select存在竞态条件 读->写)
结果为1个#  而非两个#
![image_1djbe58b51vo01manss8ll914j77d.png-38.3kB][20]

### 第二类丢失更新是经典的select ... update场景，使用乐观锁

[mapper介绍和乐观锁用法][21]

数据库新建表增加版本号并初始化值
![image_1djbf0kaf12vga1l1qq5hp3ou38k.png-6.4kB][22]

实体类增加@version
![image_1djbf4i3aespu3m5op1ov11fbl91.png-43.1kB][23]

事务1：查询原始数据（select）
![image_1djbg4keh1gqi1jlt1vqvd10r029e.png-37.6kB][24]

事务2：基于数据更新：更新操作返回值1

事务1：继续数据更新(update 与之前的select存在竞态条件 读->写)
**更新操作返回值0**
所以大可以根据返回值进行**抛出异常，自旋**等自定义的处理机制

### 可重复快照读/可重复当前读均可避免（select...update）
测试过程略

### 对于select...insert的 可重复读插入型问题
1. 主键重复会报错
2. **使用mapper封装insert ignore 解决**

(1) 使用insert ignore定义MySqlHelper:
![image_1djbhnua91pd71non1artoc4bf6a8.png-24.2kB][25]

(2) 定义使用自定义SqlHelper的Provider
![image_1djbij6ke1lm21glc1q7s10231tagc5.png-72.2kB][26]

(3) 定义使用自定义provider的Mapper
![image_1djbioum7mkd1vm812a71b4qqvgci.png-32.3kB][27]

(4) BaseMapper继承接口
![image_1djbir2pk3rd1bq01kdtgup1vavdf.png-14.9kB][28]

(5) BaseService封装mapper方法
![image_1djbit2c7rlc1qmondg18hrdseds.png-54.1kB][29]

> 对skey加上唯一索引（因为此表是自增主键ID，无法利用其主键唯一索引）

事务1：执行第一次插入返回1
![image_1djbk1moe1q71g14qboljc1hrcep.png-27.2kB][30]

事务2：再次执行savenx （实际上没有插入），返回0
**可以根据此返回值抛出异常等自定义处理，不同于乐观锁，不适合自旋**
![image_1djbk31si1nsis28b3iov0djqf6.png-33.5kB][31]



### 对于select...delete的 可重复读删除型问题
1 该操作是幂等的（不会报错）
2 使用乐观锁机制可以使其返回0

事务1 读取原数据
![image_1djbhcnhc9b41b9m10fah5119nb9r.png-44.9kB][32]

事务2  删除行记录：删除操作返回值1

事务1：删除get存在的数据（竞态条件）：删除操作返回值0

# spring-bean加载

- beanFactory加载  
bean加载首先是对beanfactory加载，beanFactory
BeanFactoryPostProcessor 就提供了一种扩展方式，可以在bean加载之前执行某些逻辑（但是需要与bean状态无关，因为它不会加载bean）

```
@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    
	@Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("可以执行bean的方法，但不会加载bean");
    }
}
```
-  **bean装载**
1. 实例化; 
2. 设置属性值; 
3. 如果实现了BeanNameAware接口,调用setBeanName设置Bean的ID或者Name; 
4. 如果实现BeanFactoryAware接口,调用setBeanFactory 设置BeanFactory; 
5. 如果实现ApplicationContextAware,调用setApplicationContext设置ApplicationContext 
6. 调用BeanPostProcessor的预先初始化方法; 
7. 调用InitializingBean的afterPropertiesSet()方法; 
8. 调用定制init-method方法； 
9. 调用BeanPostProcessor的后初始化方法;

被**@PostConstruct修饰的方法会在服务器加载Servle的时候运行**，并且只会被服务器执行一次。PostConstruct在构造函数之后执行,init()方法之前执行。

该方法在初始化的依赖注入操作之后被执行。这个方法必须在class被放到service之后被执行，这个注解所在的类必须支持依赖注入。
PostConstruct修饰的方法，要注意的是这个方法在对象的初始化和依赖都完成之后才会执行

**测试**得出：
构造方法 > @Autowired > @PostConstruct


### postconstruct顺序导致的错误
@postconstruct会在加载bean的时候执行，但是bean加载之间有先后关系。如果beanA的@postconstruct内调用beanB的实例，
如果beanB的实例也是在@PostConstruct内初始化的，那么在beanA先于beanB加载的时候，可能发生空指针异常

![image_1djt21ov3leo8vp1q891q1gqpa9.png-87.9kB][33]

### 使用autowired解决

间接通过引用指定了bean加载的顺序
```
@Service
public class PostConstructWithAutowired {

	@Autowired
	private CacheUtils cacheUtils;
	
	/**
	 * 因为有@Autowired，所以此方法不会导致启动报错
	 */
	@PostConstruct
	public void initialize() {
		System.out.println("PostConstructError's init method");
		CacheUtils.set("初始化数据key", "初始化数据value");
	}
}
```

### 使用DependsOn解决

指定了bean加载的相对顺序
```
@Service
@DependsOn("cacheUtils")
public class PostConstructWithDependsOn {

	/**
	 * 因为有@DependsOn，所以此方法不会导致启动报错
	 */
	@PostConstruct
	public void initialize() {
		System.out.println("PostConstructError's init method");
		CacheUtils.set("初始化数据key", "初始化数据value");
	}
}
```

  [1]: https://github.com/zhangtianyi123/spring/blob/master/spring/src/main/java/zty/practise/spring/SpringApp.java
  [2]: http://static.zybuluo.com/zhangtianyi/yi0etpkmh3mtfiqpembzyq6z/image_1dj8c2doa40pks1fpblk15ff9.png
  [3]: http://static.zybuluo.com/zhangtianyi/2e19y2e6flatr7nogoka8z5u/image_1dj8f4map13nh1l7511h712ak7o016.png
  [4]: https://github.com/zhangtianyi123/spring/blob/master/spring/src/main/java/zty/practise/spring/test/scope/GlobalizationByRedisService.java
  [5]: https://github.com/zhangtianyi123/spring/blob/master/spring/src/main/java/zty/practise/spring/test/scope/GlobalizationByRedisService.java
  [6]: http://static.zybuluo.com/zhangtianyi/cihiyg1brj2b9rubff0zivft/image_1djat9fa618dt10h4dj489k12qe19.png
  [7]: http://static.zybuluo.com/zhangtianyi/42aepx3ycyrsgz7c76c6vtlt/image_1djb1p7h31cb5mmjbv614eg1mnq1m.png
  [8]: http://static.zybuluo.com/zhangtianyi/7wf88r6o5pd5ocnysetoxxao/image_1djb2tr30148it6gea81cs0tpg4o.png
  [9]: http://static.zybuluo.com/zhangtianyi/i35zb8wuaagkh73rv4izp8n5/image_1djb2sin11mv48in1hv0sh05t4b.png
  [10]: http://static.zybuluo.com/zhangtianyi/ynrnq4dkssotqykguokchj93/image_1djb42lnh1s081k7pqbl11u32pr9.png
  [11]: http://static.zybuluo.com/zhangtianyi/z1ggpgz7z9ecpo7hm02lrncf/image_1djb449241r7lkd77j1s701ngm.png
  [12]: http://static.zybuluo.com/zhangtianyi/2j5zfrreld1bsn91de0epafz/image_1djb48nhv16fj56al97hjp1euo13.png
  [13]: http://static.zybuluo.com/zhangtianyi/107o2k9surbgfmp71ejybexl/image_1djb59u6ufur18eg1fquoal12sp3i.png
  [14]: http://static.zybuluo.com/zhangtianyi/oj2d95797anxxz83y4eomiad/image_1djbaoura1l011aqalkc1rdl33v.png
  [15]: http://static.zybuluo.com/zhangtianyi/qdmv25354gm2tijb2y9x9n70/image_1djbcv0ot12e01lphib0i71cac4s.png
  [16]: http://static.zybuluo.com/zhangtianyi/9drmm77ogz6rqte3hdfn06ft/image_1djbd0lag1o3h1kam1up09u5edt59.png
  [17]: http://static.zybuluo.com/zhangtianyi/9l36ldy6gwkn4klvsbjvrxyc/image_1djbd3qvjtad1t61lt01s43126i5m.png
  [18]: http://static.zybuluo.com/zhangtianyi/ctcd8lzng45pxrh3lxb08217/image_1djbdv9hm1ojj3phse81nkb1rl6j.png
  [19]: http://static.zybuluo.com/zhangtianyi/adq0v8dioavcwhvlimdfroyq/image_1djbe1ob8opor01jkc28l1nep70.png
  [20]: http://static.zybuluo.com/zhangtianyi/vp9ghrwqebabenvbj6geyz6q/image_1djbe58b51vo01manss8ll914j77d.png
  [21]: https://github.com/abel533/Mapper/wiki/2.4-version
  [22]: http://static.zybuluo.com/zhangtianyi/y3ot4t0ewxhvu3du0f7vdwt2/image_1djbf0kaf12vga1l1qq5hp3ou38k.png
  [23]: http://static.zybuluo.com/zhangtianyi/3pzjscx6tpt9yzzfyuwt8738/image_1djbf4i3aespu3m5op1ov11fbl91.png
  [24]: http://static.zybuluo.com/zhangtianyi/cvb7ydnrm0w41izklk4z0srh/image_1djbg4keh1gqi1jlt1vqvd10r029e.png
  [25]: http://static.zybuluo.com/zhangtianyi/7e0yq99sgpl0w9ko7oryqggi/image_1djbhnua91pd71non1artoc4bf6a8.png
  [26]: http://static.zybuluo.com/zhangtianyi/4cvg9ntaj8oh17q7zg35vqh5/image_1djbij6ke1lm21glc1q7s10231tagc5.png
  [27]: http://static.zybuluo.com/zhangtianyi/pq9ht6mcl38e2v9wsqohwrkv/image_1djbioum7mkd1vm812a71b4qqvgci.png
  [28]: http://static.zybuluo.com/zhangtianyi/w0lonmmg9hr6rtsufqcjzqhf/image_1djbir2pk3rd1bq01kdtgup1vavdf.png
  [29]: http://static.zybuluo.com/zhangtianyi/6fsw69wf9kz661pt25int01b/image_1djbit2c7rlc1qmondg18hrdseds.png
  [30]: http://static.zybuluo.com/zhangtianyi/rzdysgdabkmad5oont6pomx1/image_1djbk1moe1q71g14qboljc1hrcep.png
  [31]: http://static.zybuluo.com/zhangtianyi/l47zlvfez14bnde2kyj4wzex/image_1djbk31si1nsis28b3iov0djqf6.png
  [32]: http://static.zybuluo.com/zhangtianyi/s4oju3o4qjt3bcne4d1mgdwb/image_1djbhcnhc9b41b9m10fah5119nb9r.png
  [33]: http://static.zybuluo.com/zhangtianyi/2w8qs61xlkjptmd8ugc69gcc/image_1djt21ov3leo8vp1q891q1gqpa9.png