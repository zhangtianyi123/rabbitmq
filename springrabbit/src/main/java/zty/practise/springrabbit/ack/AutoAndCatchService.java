package zty.practise.springrabbit.ack;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import zty.practise.springrabbit.model.RequestEntity;
import zty.practise.springrabbit.model.ResponseEntity;
import zty.practise.springrabbit.produce.ServerSendManager;

@Service("autoAndCatchService")
public class AutoAndCatchService {
	
	private Logger logger = LoggerFactory.getLogger(AutoAndCatchService.class);
	
	public ResponseEntity doPressureTest(RequestEntity entity) {
		Date startTime = new Date();
		ResponseEntity retParams = new ResponseEntity();
		try {
			logger.info("receive = {}", entity);
			//获取DTO数据
			String reqId = entity.getReqId();
			String lotName = entity.getLotName();
			String procName = entity.getProcName();
			String eventName = entity.getEventName();
			
			//截取处lotName的前缀，这个操作依赖于lotName数据的约定，如果为Empty的数据即为脏数据，会发生RTE
			String lotHead = lotName.substring(0, 2);
			logger.info("lotHead = {}", lotHead);

			Date endTime = new Date();
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
