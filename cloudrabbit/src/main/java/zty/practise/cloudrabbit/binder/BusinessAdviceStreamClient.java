package zty.practise.cloudrabbit.binder;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface BusinessAdviceStreamClient {

	String INPUT = "inputBusinessAdvice";

    String OUTPUT = "outputBusinessAdvice";

    @Input(BusinessAdviceStreamClient.INPUT)
    SubscribableChannel input();

    @Output(BusinessAdviceStreamClient.OUTPUT)
    MessageChannel output();
}
