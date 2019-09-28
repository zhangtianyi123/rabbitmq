package zty.practise.springrabbit.ack;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zty.practise.springrabbit.model.RequestEntity;
import zty.practise.springrabbit.model.ResponseEntity;

@Service
public class AutoNoCatchService {

	private Logger logger = LoggerFactory.getLogger(AutoNoCatchService.class);

	/**
	 * 没有catch  发生RTE会抛出异常
	 * @param entity
	 * @return
	 */
	public ResponseEntity doPressureTest(RequestEntity entity) {
		Date startTime = new Date();
		ResponseEntity retParams = new ResponseEntity();
		logger.info("receive = {}", entity);
		// 获取DTO数据
		String reqId = entity.getReqId();
		String lotName = entity.getLotName();
		String procName = entity.getProcName();
		String eventName = entity.getEventName();

		// 截取lotName的前缀，这个操作依赖于lotName数据的约定，如果为Empty的数据即为脏数据，会发生RTE
		String lotHead = lotName.substring(0, 2);
		logger.info("lotHead = {}", lotHead);

		Date endTime = new Date();
		long costTime = endTime.getTime() - startTime.getTime();

		retParams.setReqId(reqId);
		retParams.setCostTime(costTime);
		retParams.setRetCode(1);

		return retParams;
	}

}
