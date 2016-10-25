package savemgo.nomad.campbell;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class Campbell {

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

	public Map<String, Object> getResponse(String page, String command, Map<String, String> data) {
		CloseableHttpClient client = HttpClients.createDefault();
		try {
			if (baseUrl.isEmpty()) {
				return ImmutableMap.of("result", "ERR_RESPONSE_NOURL");
			}

			HttpPost post = new HttpPost(baseUrl + page);

			data.put("command", command);

			if (!apiKey.isEmpty()) {
				data.put("key", apiKey);
			}

			String json = gson.toJson(data);
			StringEntity entity = null;
			try {
				entity = new StringEntity(json);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return ImmutableMap.of("result", "ERR_RESPONSE_ENCODE");
			}

			post.setEntity(entity);
			post.setHeader("Accept", "application/json");
			post.setHeader("Content-type", "application/json");

			String responseBody = "";
			try {
				CloseableHttpResponse response = client.execute(post);
				responseBody = EntityUtils.toString(response.getEntity());
			} catch (IOException e) {
				e.printStackTrace();
				return ImmutableMap.of("result", "ERR_RESPONSE_EMPTY");
			}

			System.out.println(responseBody);

			try {
				return gson.fromJson(responseBody, TYPE_HASHMAP);
			} catch (JsonParseException e) {
				e.printStackTrace();
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
