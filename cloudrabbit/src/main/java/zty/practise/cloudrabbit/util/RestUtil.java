package zty.practise.cloudrabbit.util;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RestUtil {

	public int getInstanceCount() {
		RestTemplate restTemplate = new RestTemplate();
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(parameters,
				headers);

		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor("guest", "guest"));

		String jsonString = restTemplate
				.exchange("http://localhost:15672/api/exchanges/vhost_test01/businessAdviceDestination/bindings/source",
						HttpMethod.GET, entity, String.class)
				.getBody();

		ObjectMapper mapper = new ObjectMapper();

		JsonNode arrNode = null;
		try {
			arrNode = mapper.readTree(jsonString);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(arrNode.size());
		return arrNode.size();
	}
	
}
