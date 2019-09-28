package zty.practise.springrabbit.model;

public class ResponseEntity {

	private String reqId;

	private Long costTime;

	private Integer retCode;

	public String getReqId() {
		return reqId;
	}

	public void setReqId(String reqId) {
		this.reqId = reqId;
	}

	public Long getCostTime() {
		return costTime;
	}

	public void setCostTime(Long costTime) {
		this.costTime = costTime;
	}

	public Integer getRetCode() {
		return retCode;
	}

	public void setRetCode(Integer retCode) {
		this.retCode = retCode;
	}

	@Override
	public String toString() {
		return "ResponseEntity [reqId=" + reqId + ", costTime=" + costTime + ", retCode=" + retCode + "]";
	}

}
