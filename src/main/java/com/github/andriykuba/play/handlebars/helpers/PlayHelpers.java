package com.github.andriykuba.play.handlebars.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.github.andriykuba.play.handlebars.HandlebarsApi;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Options;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import play.Environment;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.mvc.Call;
import controllers.AssetsFinder;

/**
 * Helpers specific for the Play.
 *
 */
public final class PlayHelpers {

  // Guava cache is a thread-safe so we can use it here with no doubt.
  final LoadingCache<String, CharSequence> reverseRoutingCache;
  final LoadingCache<String, CharSequence> assetsRoutingCache;

  final MessagesApi messagesApi;
  final AssetsFinder assetsFinder;
  
  final private static Splitter argumentsSplitter = 
      Splitter.on(Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"));

  /**
   * MessagesApi is a singleton so we can use it in helpers.
   * Provided AssetsFinder will be used in in all assets helper
   * 
   * @param messagesApi
   * 	MessagesApi, used in the message helper.
   * @param assetsFinder
   *  AssetsFinder, used in the assets helper. 
   * @param environment
   *  Environment, used for getting class loader.
   */
  public PlayHelpers(
      final MessagesApi messagesApi, 
      final AssetsFinder assetsFinder, 
      final Environment environment) {
    this.messagesApi = messagesApi;
    this.assetsFinder = assetsFinder;
    
    final ClassLoader classLoader = environment == null ? null : environment.classLoader(); 
    
    // Initialize the reverse router cache.
    reverseRoutingCache = CacheBuilder.newBuilder().build(
        new CacheLoader<String, CharSequence>() {
          public CharSequence load(String key) throws Exception {
            return PlayHelpers.loadRoute(key, classLoader);
          }
        });
    
    // Initialize the assets router cache.
    assetsRoutingCache = CacheBuilder.newBuilder().build(
        new CacheLoader<String, CharSequence>() {
          public CharSequence load(String key) throws Exception {
            return PlayHelpers.loadAsset(key, assetsFinder);
          }
        });
  }

  /**
   * Replacement of the Twirl's "@routes.Assets.versioned".
   * 
   * @param url
   * 	relative path to the asset.
   * @return 
   * 	actual path to the asset.
   * @throws Exception
   * 	Any exception in the case of resolving assets URL 
   */
  public CharSequence asset(final String url) throws Exception {
    return assetsRoutingCache.get(url);
  }
  
  /**
   * Called by the cache loader. Do the same as {@link #asset(String) asset} 
   * 
   * @param path
   * 	Path of the assets
   * @param assetsFinder
   *  AssetsFinder to find assets
   * @return
   * 	Real URL of the assets
   * @throws Exception
   * 	Any exception in the case of resolving assets URL
   */
  public static CharSequence loadAsset(final String path, final AssetsFinder assetsFinder) 
      throws Exception {
    return assetsFinder.path(path);
  }

  /**
   * The same as the reverse routing {@code 
   * <full-package-name>.routes.<controller>.<action>
   * } but does not need the ".routes." part in the path.
   * 
   * Reflection used for get the reverse routing. Cache used for the
   * optimization.
   * 
   * @param action
   *	Action, like {@code <full-package-name>.<controller>.<action>}.
   *    Only {@link String} and {@link Integer} action arguments type are
   *    supported. String must not contain a comma symbol, like ",".
   * @param options
   * 	Object for getting context to resolve handlebar variables in method signature.
   * @return 
   * 	URL that correspond to the action
   * @throws Exception
   * 	any exception in the cache
   */
  public CharSequence route(final String action, final Options options) throws Exception {
	String actionReolved = resolveContextVariables(action.trim(), options.context);
    return reverseRoutingCache.get(actionReolved);
  }

  /**
   * Called by the cache loader. Do the same as {@link #route(String) route} 
   * 
   * @param action
   * @return
   * @throws Exception
   */
  private static CharSequence loadRoute(final String action, final ClassLoader classLoader) 
      throws Exception {
    // Trim the string to avoid nasty space mistakes.
    final int signatureStart = action.indexOf('(');
    final String actionWithoutArguments = 
    		(signatureStart > 0 ? action.substring(0, signatureStart) : action).trim();
    		
    // Divide the method call from the class path.
    final String[] methodSplitment = splitStringByLastDot(actionWithoutArguments);
    final String methodName = methodSplitment[1];

    // Divide the class from the path.
    final String[] classSplitment = splitStringByLastDot(methodSplitment[0]);

    // Get the method and its arguments
    RouteMethodArguments methodArguments;
    if (signatureStart > 0) {
      // Possible arguments are present.
      final String parametersString = action.substring(signatureStart + 1, action.lastIndexOf(')'));
      methodArguments = parseMethodArguments(parametersString);
    } else {
      methodArguments = new RouteMethodArguments(null, null);
    }

    // Return the action URL
    return reverseUrl(
        classLoader, 
        classSplitment[0], 
        classSplitment[1], 
        methodName, 
        methodArguments);
  }

  /**
   * Get the URL by the controllers package, class, method and parameters. It
   * use reflection for get the reverse route.
   * 
   * @param controllerPackage
   * 	The name of the package of the controller
   * @param controllerClass
   * 	The name of the controller class
   * @param methodName
   * 	The method name in the controller, i.e. action
   * @param methodArguments
   * 	The arguments of the method
   * @return
   * 	Reversed URL
   * @throws Exception
   * 	Any exception in the case of reversion
   */
  private static String reverseUrl(
      final ClassLoader classLoader,
      final String controllerPackage,
      final String controllerClass,
      final String methodName,
      final RouteMethodArguments methodArguments) throws Exception {

    // Load the auto generated class "routes".
    final Class<?> routerClass = classLoader.loadClass(controllerPackage + ".routes");

    // Get the reverse router object of the controller.
    final Field declaredField = routerClass.getDeclaredField(controllerClass);
    // It's static field.
    final Object object = declaredField.get(null);
    final Class<?> type = declaredField.getType();

    // Get the action of the reverse controller.
    final Method routerMethod = type.getMethod(methodName, methodArguments.types);
    final Call invoke = (Call) routerMethod.invoke(object, methodArguments.values);

    // Get the URL of the action.
    return invoke.url();
  }

  /**
   * Parse the method arguments. Only String and Integers are legal parameters.
   * No parameters are allowed, but an empty parameter is not allowed
   * 
   * @param argumentsString
   * 	The method arguments as string
 * @param context 
 * @param context 
   * @return
   * 	Parsed route arguments
   */
  private static RouteMethodArguments parseMethodArguments(String argumentsString) {
    if (argumentsString.trim().length() == 0) {
      // Method with empty braces - no arguments
      return new RouteMethodArguments(null, null);
    }

    final  Iterator<String> iterator = argumentsSplitter.split(argumentsString).iterator();

    final List<String> arguments =  new ArrayList<String>();
    while (iterator.hasNext()) {
      arguments.add(iterator.next());
    }
    
    final List<Class<?>> types = new ArrayList<>();
    final List<Object> values = new ArrayList<>();

    for (String argument: arguments) {
      // Normalize argument
      argument = argument.trim();

      if (argument.length() == 0) {
        // Empty argument is not allowed
        throw new RuntimeException("An empty argument");
      }

      // Detect the argument types and values
      if (argument.startsWith("\"") && argument.endsWith("\"")) {
        // The string argument
        types.add(String.class);
        values.add(argument.substring(1, argument.length() - 1));
      } else {
        try {
          // Try the integer argument
          Integer valueInteger = Integer.parseInt(argument);
          types.add(Integer.class);
          values.add(valueInteger);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Unsupported argument format. Only String and Integer are supported", e);
        }
      }
    }

    return new RouteMethodArguments(
        types.toArray(new Class[types.size()]),
        values.toArray(new Object[values.size()]));
  }
  
  /**
   * Replace the action arguments with the correspondence context variables.
   *
   * @param action
   * 	action to process.
   * @param context
   * 	handlebars context.
   * @return
   * 	action with replaced arguments (if needed)
   */
  private static String resolveContextVariables(final String action, final Context context){
  	// Take the variable from the context
	  final int start = action.indexOf('(');
	  if(start < 0) return action;
	  
	  final int end = action.lastIndexOf(')');
	  final String argumentsString = action.substring(start + 1, end);
	  final String actionPath = action.substring(0, start);
	  
	  final String[] arguments = argumentsString.split(",");
	  for (int i = 0; i < arguments.length; i++) {
	      // Normalize argument
	      String argument = arguments[i].trim();
	      if (!argument.startsWith("\"") || !argument.endsWith("\"")) {
	          try {
	            // Check if argument is integer
	            Integer.parseInt(argument);
	          } catch (NumberFormatException e) {
	        	Object value = context.get(argument);
	        	if(value instanceof Integer){
	        		arguments[i] = value.toString();
	        	}else{
	        		arguments[i] = "\"" + value.toString() + "\"";
	        	}
	          }
	        }
	  }
	  return actionPath + "(" + String.join(",", arguments) + ")";
  }

  /**
   * The method arguments; types and values.
   *
   */
  private static class RouteMethodArguments {
    final Class<?>[] types;
    final Object[] values;

    RouteMethodArguments(Class<?>[] types, Object[] values) {
      this.types = types;
      this.values = values;
    }
  }

  /**
   * Split the string by the last dot.
   * 
   * @param string
   * 	String to split
   * @return 
   * 	index 0 - The part of string before the dot. index 1 - The part of
   *    string after the dot.
   */
  private static String[] splitStringByLastDot(String string) {
    String[] splitted = new String[2];

    final int point = string.lastIndexOf('.');

    if (point < 0) {
      throw new RuntimeException("String \"" + string + "\" must contain dot");
    }

    splitted[0] = string.substring(0, point);
    splitted[1] = string.substring(point + 1);

    return splitted;
  }

  /**
   * Do the same as "@Message(key)" in Twirl. It use MessageFormat for the
   * formatting as well as "@Message(key)".
   * 
   * @param key
   * 	message key in the messages.** files.
   * @param options
   * 	message options, just like in the "@Message"      
   * @return message
   * 	the message in the context language
   */
  public CharSequence message(final Object key, final Options options) {
	// Get the current language.
	String languageCode = options.context.get(HandlebarsApi.LANGUAGE_PROPERTY).toString();
	Lang lang = Lang.forCode(languageCode);
	// Retrieve the message, internally formatted by MessageFormat.
    return messagesApi.get(lang, key.toString(), options.params);
  }

}
