package zty.practise.bootrabbit.consume;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zty.practise.bootrabbit.model.RequestEntity;
import zty.practise.bootrabbit.model.ResponseEntity;

@Service("pressureTestService")
@Slf4j
public class PressureTestService {
	
	public ResponseEntity doPressureTest(RequestEntity entity) {
		// 获取开始时间
		Date startTime = new Date();
		ResponseEntity retParams = new ResponseEntity();
		log.info("receive = {}", entity);
		// 获取json数据
		String reqId = entity.getReqId();
		String lotName = entity.getLotName();
		String procName = entity.getProcName();
		String eventName = entity.getEventName();
			
		// 截取lotName的前缀，这个操作依赖于lotName数据的约定，如果为Empty的数据即为脏数据，会发生RTE
		String lotHead = lotName.substring(0, 2);
		log.info("lotHead = {}", lotHead);

		// API执行完成时间
		Date endTime = new Date();
		// 计算执行API使用的时间
		long costTime = endTime.getTime() - startTime.getTime();

		retParams.setReqId(reqId);
		retParams.setCostTime(costTime);
		retParams.setRetCode(1);
		
		return retParams;
	}

	
}
