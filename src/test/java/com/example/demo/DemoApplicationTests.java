package com.example.demo;

import com.example.demo.constants.OpenAiModels;
import com.example.demo.util.SHA256HashingUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionChunk;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import com.theokanning.openai.completion.chat.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Slf4j
class DemoApplicationTests {

	String appId = "YOUR APPID";
	String appSecret = "YOUR APPSECRET";

	String ENDPOINT = "https://node1.gptforcn.com";

	String URL_TEST = ENDPOINT + "/v1/test";
	String URL_MODELS = ENDPOINT + "/v1/models";
	String URL_COMPLETION = ENDPOINT +"/v1/completions";
	String URL_CHAT_COMPLETION = ENDPOINT +"/v1/chat/completions";

	/**
	 * Test your app id and app secret
	 * @throws Exception
	 */
	@Test
	void test() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		String data = "test";
		HttpHeaders headers = buildHeaders(appId, appSecret, data);
		HttpEntity<String> entity = new HttpEntity<>(data, headers);
		String response = restTemplate.postForObject(URL_TEST, entity, String.class);
		log.info("test 返回： {}", response);
		assertNotNull(response);
	}

	/**
	 * Test models
	 * @throws Exception
	 */
	@Test
	void models() throws Exception {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 1080));
		requestFactory.setProxy(proxy);

		RestTemplate restTemplate = new RestTemplate(requestFactory);
		String response = restTemplate.getForObject(URL_MODELS, String.class);
		log.info("models 返回： {}", response);
		assertNotNull(response);
	}

	/**
	 * Test gpt completion function, models DAVINCI, CURIE, ADA available
	 * @throws Exception
	 */
	@Test
	void completion() throws Exception {
		CompletionRequest completionRequest = CompletionRequest.builder()
				.prompt("hello")
				.maxTokens(1000)
				.model(OpenAiModels.CURIE)
				.temperature(0.5)
				.build();
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = buildHeaders(appId, appSecret, completionRequest);
		HttpEntity<CompletionRequest> entity = new HttpEntity<>(completionRequest, headers);
		String response = restTemplate.postForObject(URL_COMPLETION, entity, String.class);
		log.info("completion 返回： {}", response);

		ObjectMapper objectMapper = new ObjectMapper();
		CompletionResult result = objectMapper.readValue(response, CompletionResult.class);
		assertNotNull(result);
	}

	@Test
	void testCompletionStream() throws Exception {
		CompletionRequest completionRequest = CompletionRequest.builder()
				.prompt("墨尔本是哪里")
				.maxTokens(100)
				.model(OpenAiModels.CURIE)
				.temperature(0.5)
				.build();

		URL url = new URL(URL_COMPLETION);

		ObjectMapper mapper = new ObjectMapper();
		String data = mapper.writeValueAsString(completionRequest);
		long timestamp = System.currentTimeMillis()/1000;
		//校验签名 拼接请求内容按照字母排序进行sha256签名
		String unSignStr = "appId=" + appId + "&appSecret=" + appSecret + "&data=" + data + "&timestamp=" + timestamp;
		log.info("等待签名数据: " + unSignStr);
		String sign = SHA256HashingUtil.sha256Hash(unSignStr);
		log.info("签名: " + sign);

		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Accept", "text/event-stream");
		connection.setRequestProperty("content-type", "application/json; charset=UTF-8");
		connection.setRequestProperty("appId", appId);
		connection.setRequestProperty("timestamp", timestamp + "");
		connection.setRequestProperty("sign", sign);

		connection.setDoOutput(true);

		// Send POST request
		connection.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));

		// Read server response
		InputStream inputStream = connection.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		StringBuilder content = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			// Process server response
			if (StringUtils.isNotBlank(line)){
				CompletionChunk completionChunk = mapper.readValue(line, CompletionChunk.class);
				for (CompletionChoice choice : completionChunk.getChoices()) {
					if (choice.getText() != null){
						content.append(choice.getText());
					}
				}
			}
			log.info(line);
		}

		log.info(content.toString());
		// Close connections
		reader.close();
		inputStream.close();
		connection.disconnect();
	}

	@Test
	void testChatCompletionStream() throws Exception {
		List<ChatMessage> messages = new ArrayList<>();
		messages.add(new ChatMessage("user", "介绍下上海"));
		ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
				.messages(messages)
				.maxTokens(1000)
				.model(OpenAiModels.GPT_3_5)
				.temperature(0.5)
				.build();

		URL url = new URL(URL_CHAT_COMPLETION);

		ObjectMapper mapper = new ObjectMapper();
		String data = mapper.writeValueAsString(chatCompletionRequest);
		long timestamp = System.currentTimeMillis()/1000;
		//校验签名 拼接请求内容按照字母排序进行sha256签名
		String unSignStr = "appId=" + appId + "&appSecret=" + appSecret + "&data=" + data + "&timestamp=" + timestamp;
		log.info("等待签名数据: " + unSignStr);
		String sign = SHA256HashingUtil.sha256Hash(unSignStr);
		log.info("签名: " + sign);

		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Accept", "text/event-stream");
		connection.setRequestProperty("content-type", "application/json; charset=UTF-8");
		connection.setRequestProperty("appId", appId);
		connection.setRequestProperty("timestamp", timestamp + "");
		connection.setRequestProperty("sign", sign);

		connection.setDoOutput(true);

		// Send POST request
		connection.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));

		// Read server response
		InputStream inputStream = connection.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		StringBuilder content = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			// Process server response
			if (StringUtils.isNotBlank(line)){
				ChatCompletionChunk chatCompletionChunk = mapper.readValue(line, ChatCompletionChunk.class);
				for (ChatCompletionChoice choice : chatCompletionChunk.getChoices()) {
					if (choice.getMessage().getContent() != null){
						content.append(choice.getMessage().getContent());
					}
				}
			}
			log.info(line);
		}

		log.info(content.toString());
		// Close connections
		reader.close();
		inputStream.close();
		connection.disconnect();
	}

	/**
	 * Test gpt chat completion function, models GPT_3_5 available
	 * @throws Exception
	 */
	@Test
	void chatCompletion() throws Exception {
		List<ChatMessage> messages = new ArrayList<>();
		messages.add(new ChatMessage("user", "hello"));
		ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
				.messages(messages)
				.maxTokens(1000)
				.model(OpenAiModels.GPT_3_5)
				.temperature(0.5)
				.build();

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = buildHeaders(appId, appSecret, chatCompletionRequest);
		HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(chatCompletionRequest, headers);
		String response = restTemplate.postForObject(URL_CHAT_COMPLETION, entity, String.class);
		log.info("chat completion 返回： {}", response);

		ObjectMapper objectMapper = new ObjectMapper();
		ChatCompletionResult result = objectMapper.readValue(response, ChatCompletionResult.class);
		assertNotNull(result);
	}

	/**
	 * 构建签名并写入headers
	 * @param appId
	 * @param appSecret
	 * @param value
	 * @return
	 * @throws JsonProcessingException
	 * @throws NoSuchAlgorithmException
	 */
	private HttpHeaders buildHeaders(String appId, String appSecret, Object value) throws JsonProcessingException, NoSuchAlgorithmException {
		ObjectMapper mapper = new ObjectMapper();
		String data = value ==null? "" : mapper.writeValueAsString(value);
		long timestamp = System.currentTimeMillis()/1000;
		//校验签名 拼接请求内容按照字母排序进行sha256签名
		String unSignStr = "appId=" + appId + "&appSecret=" + appSecret + "&data=" + data + "&timestamp=" + timestamp;
		log.info("等待签名数据: " + unSignStr);
		String sign = SHA256HashingUtil.sha256Hash(unSignStr);
		log.info("签名: " + sign);
		HttpHeaders headers = new HttpHeaders();
		MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
		headers.setContentType(type);
		headers.add("appId", appId);
		headers.add("timestamp", timestamp + "");
		headers.add("sign", sign);
		return headers;
	}
}
