package zty.practise.bootrabbit.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class ResponseEntity implements Serializable {

	private static final long serialVersionUID = -4805468244533700826L;

	private String reqId;

	private Long costTime;

	private Integer retCode;
}
