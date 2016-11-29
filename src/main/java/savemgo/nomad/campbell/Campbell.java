package savemgo.nomad.campbell;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class Campbell {

	private static final Logger logger = LogManager.getLogger(Campbell.class.getSimpleName());

	private static final Type TYPE_HASHMAP = new TypeToken<Map<String, Object>>() {
	}.getType();

	private static Campbell instance = null;

	private final Gson gson = new Gson();

	private String baseUrl = "";
	private String apiKey = "";

	private Campbell() {

	}

	public static Campbell instance() {
		if (instance == null) {
			instance = new Campbell();
		}
		return instance;
	}

	public Map<String, Object> getResponse(String page, String command) {
		return getResponse(page, command, new HashMap<>());
	}

	public Map<String, Object> getResponse(String page, String command, Map<String, Object> data) {
		CloseableHttpClient client = HttpClients.createDefault();
		try {
			if (baseUrl.isEmpty()) {
				return ImmutableMap.of("result", "ERR_RESPONSE_NOURL");
			}

			HttpPost post = new HttpPost(baseUrl + page);

			data.put("command", command);

			if (!apiKey.isEmpty()) {
				data.put("apikey", apiKey);
			}

			String json = gson.toJson(data);

			logger.debug(json);
			
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("msg", json));

			HttpEntity entity = null;
			try {
				entity = new UrlEncodedFormEntity(params);
			} catch (UnsupportedEncodingException e) {
				logger.error("Failed to encode data.", e);
				return ImmutableMap.of("result", "ERR_RESPONSE_ENCODE");
			}

			post.setEntity(entity);

			String responseBody = "";
			try {
				CloseableHttpResponse response = client.execute(post);
				responseBody = EntityUtils.toString(response.getEntity());
			} catch (IOException e) {
				logger.error("Failed to execute post.", e);
				return ImmutableMap.of("result", "ERR_RESPONSE_EMPTY");
			}

			logger.debug(responseBody);

			try {
				return gson.fromJson(responseBody, TYPE_HASHMAP);
			} catch (JsonParseException e) {
				logger.error("Failed to parse response.", e);
				return ImmutableMap.of("result", "ERR_RESPONSE_PARSE");
			}
		} finally {
			try {
				client.close();
			} catch (IOException e) {
			}
		}
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

}
