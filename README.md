# api-gptforcn
##接口使用说明
> gptforcn 是方便中国大陆快速对接openai接口的工具，使用前请先<a href="https://gptforcn.com/">获取</a>appId与secret

## 签名方式介绍
data：待签名数据，jsonstring格式。内容为当次请求的请求体
timestamp: 时间戳秒数
将待签名字段按照字母从小到大顺序拼接后进行sha2-256运算得到sign
``` java
String unSignStr = "appId=" + appId + "&appSecret=" + appSecret + "&data=" + data + "&timestamp=" + timestamp;

MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
messageDigest.update(unSignStr.getBytes(StandardCharsets.UTF_8));
byte[] digest = messageDigest.digest();
StringBuilder builder = new StringBuilder();
for (byte b : bytes) {
  builder.append(String.format("%02x", b));
}

String sign = builder.toString();
```
请求header中加传 appId、timestamp和sign字段，时间戳有效期半个小时之内

示例：

``` java
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
		String data = mapper.writeValueAsString(value);
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

  void test() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		String data = "test";
		HttpHeaders headers = buildHeaders(appId, appSecret, data);
		HttpEntity<String> entity = new HttpEntity<>(data, headers);
		String response = restTemplate.postForObject(URL_TEST, entity, String.class);
		log.info("test 返回： {}", response);
		assertNotNull(response);
	}
```

##api列表:
所有请求参数与返回参数均与<a href="https://platform.openai.com/docs/api-reference">OPENAI接口</a>相同, 出现错误时请参阅http错误信息

| 接口                                          | 方式 | 请求体    | 返回内容 |
| --------------------------------------------- | ------ | ---------- | ---- |
|  /v1/test                          | POST     |        | 测试签名接口，成功返回success |
|  /v1/models                        | GET      |        | Openai返回的models |
|  /v1/completions                   | POST     | CompletionRequest     | Openai的返回内容 |
|  /v1/chat/completions              | POST     | ChatCompletionRequest | Openai的返回内容 |
|  /chat-process                     | POST     | ChatCompletionRequest | 适用于<a href="https://github.com/Chanzhaoyu/chatgpt-web">Chanzhaoyu/chatgpt-web</a>的流式接口 |
|  /session                          | POST     |                       | 适用于<a href="https://github.com/Chanzhaoyu/chatgpt-web">Chanzhaoyu/chatgpt-web</a> |

##注意
> /v1/completions 与 /v1/chat/completions 均支持 text/event-stream 流式请求，具体写法可以参考代码中的案例

##关于流式接口的token计算
> 流式接口的token使用量并不会在请求过程中返回，需要自行计算，具体可参阅 <a href="https://github.com/openai/openai-cookbook/blob/main/examples/How_to_count_tokens_with_tiktoken.ipynb"> token计算方法</a>
