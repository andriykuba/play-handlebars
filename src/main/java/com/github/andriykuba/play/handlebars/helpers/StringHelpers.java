package com.github.andriykuba.play.handlebars.helpers;

import java.net.URLEncoder;
import java.util.StringJoiner;

import com.github.jknack.handlebars.Options;

/**
 * Helpers for work with strings.
 *
 */
public final class StringHelpers {
	  
  /**
   * Encode the given string so it could be used in the URL as parameter.
   * There is {@link java.net.URLEncoder#encode java.net.URLEncoder.encode} under the hood.
   * 
   * @param parameter
   * 	string that will be encoded.
   * @return
   * 	encoded string.  
   * @throws Exception
   * 	exception in the case of unable to encode.
   */
  public static CharSequence encodeUrlParameter(final Object parameter) throws Exception{
    if(parameter == null) return "";
    return URLEncoder.encode(parameter.toString(), "UTF-8");
  }
  
  /**
   * Returns value if obj1 and obj2 are equals
   * 
   * @param obj1
   * 	Object pretend to be a string.
   * @param obj2
   * 	Object pretend to be a string.
   * @param value
   * 	String representation of this object would be returned if {@code obj1} equals {@code obj2}
   * @return
   * 	{@code value} as string or an empty string.
   */
  public static CharSequence if_equals(final Object obj1, final Object obj2, final Object value){
    if(obj1 == null) {
    	return obj2 == null ? value.toString() : "";
    } 
    return obj1.equals(obj2) ? value.toString() : "";
  }
  
  /**
   * Take the string representations of the parameters and join them in to one string.
   * 
   * source: https://gist.github.com/ndemengel/b9fdd87011ebcc8be83b20820359bb5b
   * 
   * @param firstObjectToConcat  first object to concatenation
   * @param options the next objects to concatenation
   * @return resulting string
   */
  public static CharSequence concat(Object firstObjectToConcat, Options options) {
    StringJoiner joiner = new StringJoiner("");
    joiner.add(String.valueOf(firstObjectToConcat));
    
    // all params except the first one
    for (Object param : options.params) {
        joiner.add(String.valueOf(param));
    }
    return joiner.toString();
  }
}
