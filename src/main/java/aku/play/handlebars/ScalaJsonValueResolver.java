package aku.play.handlebars;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jknack.handlebars.ValueResolver;

import play.api.libs.json.JsValue;
import play.api.libs.json.JsObject;
import play.api.libs.json.JsArray;
import play.api.libs.json.JsBoolean;
import play.api.libs.json.JsNull;
import play.api.libs.json.JsNumber;
import play.api.libs.json.JsString;
import play.api.libs.json.JsUndefined;

public enum ScalaJsonValueResolver implements ValueResolver{

	INSTANCE;
	
	private static scala.Option<Object> SCALA_OPTION_NONE = scala.Option.apply(null);
	
	@Override
	public Set<Map.Entry<String, Object>> propertySet(final Object context) {
		if(context instanceof JsArray){
			final Map<String, Object> result = new LinkedHashMap<String, Object>();
			final List<JsValue> list = toJavaList((JsArray) context);
			
			int i = 0;
			for (JsValue value : list){
				result.put(Integer.toString(i++), resolve(value));
			}

			return result.entrySet();
		}
		
		if(context instanceof JsObject){
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			
			JsObject js = (JsObject) context;
			scala.collection.Map<String, JsValue> values = js.value();
			scala.collection.Iterator<scala.Tuple2<String, JsValue>> iterator = values.iterator();
			while(iterator.hasNext()){
				scala.Tuple2<String, JsValue> entity = iterator.next();
				String key = entity._1;
				Object value = resolve(entity._2, key);

				result.put(key, value);
			}
			
			return result.entrySet();
		}
		
		return Collections.emptySet();
	}

	@Override
	public Object resolve(final Object context) {
		if(context == null){
			return null;
		}
		if(context.getClass().equals(JsUndefined.class)){
			throw new RuntimeException("Value is undefined: " + context.toString());
		}
		if(context.getClass().equals(JsNull.class)){
			return null;
		}
		
		if (context instanceof JsValue) {
	        return resolveValue((JsValue) context);
		}
		return UNRESOLVED;
	}

	@Override
	public Object resolve(final Object context, final String key) {
		final Object value = resolveNullable(context ,key);
		return value == null ? UNRESOLVED : value;
	}
		
    public Object resolveNullable(final Object context, final String key) {
		if (context instanceof JsObject){
			final scala.collection.Map<String, JsValue> map = 
					((JsObject) context).play$api$libs$json$JsObject$$underlying();
			
			final scala.Option<JsValue> option = map.get(key);
			if(!SCALA_OPTION_NONE.equals(option)){
				return resolve(option.get());
			}
		}
		
		return UNRESOLVED;
	}
	
	
	private Object resolveValue(final JsValue value) {
		if(value instanceof JsObject){
			return toMap((JsObject) value);
		}
		if(value instanceof JsBoolean){
			return ((JsBoolean) value).value();
		}
		if(value instanceof JsNumber){
			return ((JsNumber) value).value();
		}
		if(value instanceof JsString){
			return ((JsString) value).value();
		}
		if(value instanceof JsArray){
			return toJavaList((JsArray) value);
		}

		return value;
	}

	 private Map<String, Object> toMap(final JsObject value) {
		final scala.collection.Map<String, JsValue> map = value.play$api$libs$json$JsObject$$underlying();
		
	    return new AbstractMap<String, Object>() {

	        @Override
	        public Object get(final Object key) {
	          final scala.Option<JsValue> option = map.get((String) key);
	          if(SCALA_OPTION_NONE.equals(option)){
	        	 return UNRESOLVED;
	          }
	          return resolve(option.get());
	        }
	        
	        @Override
	        public int size() {
	          return map.size();
	        }
	        
			@Override
			public Set<Map.Entry<String, Object>> entrySet() {
				final Set<Map.Entry<String, Object>> set = new LinkedHashSet<>();
				
				final scala.collection.Iterator<scala.Tuple2<String, JsValue>> iterator = map.iterator();
				while(iterator.hasNext()){
					final scala.Tuple2<String, JsValue> entity = iterator.next();
					final String key = entity._1;
					final Object value = entity._2;

					set.add(new SimpleEntry<String, Object>(key, value));
				}
				
				return set;
			}
	    };
	 }
	 
	 private static List<JsValue> toJavaList(final JsArray array){
		 final scala.collection.Seq<JsValue> seq = array.value();
		 return scala.collection.JavaConversions.seqAsJavaList(seq);
	 }
}
