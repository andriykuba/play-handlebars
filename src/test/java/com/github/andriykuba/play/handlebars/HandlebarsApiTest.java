package com.github.andriykuba.play.handlebars;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import play.api.libs.json.JsValue;
import play.api.libs.json.Json;
import play.i18n.MessagesApi;

public class HandlebarsApiTest {
	private HandlebarsApi api;
	private final static String LANGUAGE_CODE = "da";
	@Before
	public void initHandlebars(){
		Config config = ConfigFactory.load();
		MessagesApi messagesApi = Mockito.mock(MessagesApi.class);
		
		api = new HandlebarsApi(null, config, messagesApi);
	}
	
	@Test
	public void noValuesTemplate() {
		Map<String, Object> data = new HashMap<>();
		String template = "simple template";

		String result = api.renderInline(template, data, LANGUAGE_CODE);

 		assertEquals(template, result);
	}
	
	@Test
	public void mapResolverStringValue() {
		Map<String, Object> data = ImmutableMap.of("key", "MyKeyValue");

		String template = "simple {{key}} template";
		String correctResult = "simple MyKeyValue template";

		String result = api.renderInline(template, data, LANGUAGE_CODE);

 		assertEquals(correctResult, result);
	}
	
	@Test
	public void jsonRootArray() {
		String template = "{{#this}}{{this}}{{/this}}";
		
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode array = mapper.createArrayNode();

		for(int i=1; i<4; i++){
		    array.add(i);
		}
		String result = api.renderInline(template, array, LANGUAGE_CODE);

 		assertEquals("123", result);
	}
	
	@Test
	public void jsonObjectKey() {
		String template = "{{object.user.name}}";
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		ObjectNode object =  root.putObject("object");
		ObjectNode user = object.putObject("user");
		user.put("name", "Petro");
		user.put("age", 20);
		
		
		String result = api.renderInline(template, root, LANGUAGE_CODE);

 		assertEquals("Petro", result);
	}
	
	@Test
	public void playJsonObjectKey() {
		String template = "{{object.user.name}}";
		
		JsValue data = Json.parse("{\"object\":{\"user\":{\"name\":\"Petro\", \"age\": 20}}}");
		String result = api.renderInline(template, data, LANGUAGE_CODE);

 		assertEquals("Petro", result);
	}
	
	@Test
	public void ifEqualsTrue(){
		String template = "{{if_equals values.first values.second \"Petro\"}}";
		
		JsValue data = Json.parse("{\"values\":{\"first\":\"A\", \"second\":\"A\"}}");
		String result = api.renderInline(template, data, LANGUAGE_CODE);
		assertEquals("Petro", result);
	}
	
	@Test
	public void ifEqualsFalse(){
		String template = "{{if_equals values.first values.second \"Petro\"}}";
		
		JsValue data = Json.parse("{\"values\":{\"first\":\"A\", \"second\":\"B\"}}");
		String result = api.renderInline(template, data, LANGUAGE_CODE);
		assertEquals("", result);
	}
}
