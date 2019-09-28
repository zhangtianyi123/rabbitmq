package zty.practise.springrabbit.consume;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zty.practise.springrabbit.model.RequestEntity;
import zty.practise.springrabbit.model.ResponseEntity;
import zty.practise.springrabbit.produce.ServerSendManager;

@Service("pressureTestService")
public class PressureTestService {
	
	private Logger logger = LoggerFactory.getLogger(PressureTestService.class);
	
	public ResponseEntity doPressureTest(RequestEntity entity) {
		// 获取开始时间
		Date startTime = new Date();
		ResponseEntity retParams = new ResponseEntity();
		try {
			logger.info("receive = {}", entity);
			// 获取json数据
			String reqId = entity.getReqId();
			String lotName = entity.getLotName();
			String procName = entity.getProcName();
			String eventName = entity.getEventName();

			// API执行完成时间
			Date endTime = new Date();
			// 计算执行API使用的时间
			long costTime = endTime.getTime() - startTime.getTime();

			retParams.setReqId(reqId);
			retParams.setCostTime(costTime);
			retParams.setRetCode(1);

		} catch (Exception e) {
			retParams.setReqId(entity.getReqId());
			retParams.setCostTime(0L);
			retParams.setRetCode(0);
		}
		
		return retParams;
	}

	
}
