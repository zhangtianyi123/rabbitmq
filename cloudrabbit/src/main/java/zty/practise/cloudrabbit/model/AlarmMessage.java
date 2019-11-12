package zty.practise.cloudrabbit.model;

import java.io.Serializable;
import lombok.Data;


@Data
public class AlarmMessage implements Serializable {

	private static final long serialVersionUID = -4483928283461086946L;

	private Long alarmMessageIdentifier;

	private String alarmItemCode;
}
