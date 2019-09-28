package zty.practise.bootrabbit.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class RequestEntity implements Serializable {

	private static final long serialVersionUID = -3857650347326384083L;

	private String reqId;
	
	private String lotName;
	
	private String procName;
	
	private String opName;
	
	private String eventName;
	
}
