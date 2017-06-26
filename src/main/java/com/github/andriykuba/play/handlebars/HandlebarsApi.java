package com.github.andriykuba.play.handlebars;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.github.andriykuba.play.handlebars.helpers.PlayHelpers;
import com.github.andriykuba.play.handlebars.helpers.StringHelpers;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.GuavaTemplateCache;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.typesafe.config.Config;
import play.Environment;
import play.i18n.MessagesApi;
import play.twirl.api.Content;

/**
 * Provide access to the handlebars template engine.
 */
@Singleton
public class HandlebarsApi {

	public final static String LANGUAGE_PROPERTY = "language";

	/**
	 * Original handlebars engine.
	 */
	private final Handlebars handlebars;

	private final MessagesApi messagesApi;

	/**
	 * Initialize Handlebars engine, register cache, handlers.
	 * 
	 * @param environment
	 *            Play environment, used for getting templates folder. 
	 *            Could be null for the inline rendering.
	 * @param config
	 *            Play configuration, used for getting properties
	 * @param messagesApi
	 *            MessagesApi, used in the message helpers
	 */
	@Inject
	public HandlebarsApi(
			final Environment environment, 
			final Config config,
			final MessagesApi messagesApi) {

		this.messagesApi = messagesApi;

		// Initialize the properties.
		final Properties properties = new Properties(config);

		// Get the template folder.
		final File rootFolder = 
				(environment == null) ? null : environment.getFile(properties.getDirectory());

		// Put the template extension.
		final TemplateLoader loader = 
				(rootFolder == null) ? null : new FileTemplateLoader(rootFolder, properties.getExtension());

		// Initialize the engine with the cache
		handlebars = new Handlebars(loader);

		if (properties.isCacheEnabled()) {
			// Initialize the cache. Could be builded from configuration as well
			// For example:
			// CacheBuilder.from(config.getString("hbs.cache")).build()
			final Cache<TemplateSource, Template> cache = CacheBuilder.newBuilder()
					.expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1000).build();
			final GuavaTemplateCache guavaCache = new GuavaTemplateCache(cache);
			handlebars.with(guavaCache);
		}

		// Add helpers. 
		// MessagesApi is a singleton so we can use it in helpers.
		PlayHelpers helpers = new PlayHelpers(messagesApi);
		handlebars.registerHelpers(helpers);
		handlebars.registerHelpers(StringHelpers.class);
	}

	public MessagesApi getMessagesApi() {
		return messagesApi;
	}
	
	public Handlebars getHandlebars(){
		return handlebars;
	}

	/**
	 * Render the template with the data. 
	 * Checked exceptions converted to unchecked.
	 * 
	 * @param templateName
	 *            Name of the template to be rendered.
	 * @param data
	 *            Data to fill the template.
	 * @param languageCode
	 *            This language would be used within message helper
	 * @return Compiled and filled with data.
	 * 
	 */
	public String render(final String templateName, final Object data, final String languageCode) {
		return render(false, templateName, data, languageCode);
	}

	/**
	 * Render the inline template with the data. 
	 * Checked exceptions converted to unchecked.
	 * 
	 * @param template
	 *            Template to be rendered.
	 * @param data
	 *            Data to fill the template.
	 * @param languageCode
	 *            This language would be used within message helper
	 * @return Compiled and filled with data.
	 * 
	 */
	public String renderInline(final String template, final Object data, final String languageCode) {
		return render(true, template, data, languageCode);
	}
	
	private String render(final boolean isInline, final String source, final Object data, final String languageCode) {
		try {
			final Template template = isInline ? handlebars.compileInline(source) : handlebars.compile(source);

			final Context context = Context.newBuilder(data).combine(LANGUAGE_PROPERTY, languageCode)
					.resolver(
							ScalaJsonValueResolver.INSTANCE,
							JsonNodeValueResolver.INSTANCE, 
							MapValueResolver.INSTANCE, 
							FieldValueResolver.INSTANCE)
					.build();

			return template.apply(context);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Calls {@link #render(String, Object, String) render} method and convert
	 * result to the {@link play.twirl.api.Content Content}.
	 * 
	 * @param templateName
	 *            The name of the template to be used
	 * @param data
	 *            This data would be used within template
	 * @param languageCode
	 *            This language would be used within message helper
	 * @return The same object as standard Play template template engine returns
	 */
	public Content html(final String templateName, final Object data, final String languageCode) {
		return new HtmlContent(render(templateName, data, languageCode));
	}

	/**
	 * Proxy handlebars configuration for an easy access.
	 */
	private static class Properties {
		final static String ROOT = "handlebars";
		final static String DIRECTORY = "directory";
		final static String EXTENSION = "extension";
		final static String IS_CASHE_ENABLED = "isCacheEnabled";

		/**
		 * the handlebars configuration.
		 */
		private Config config;

		Properties(final Config config) {
			this.config = config.getConfig(Properties.ROOT);
		}

		/**
		 * @return status of the cache.
		 */
		boolean isCacheEnabled() {
			return config.getBoolean(IS_CASHE_ENABLED);
		}

		/**
		 * @return the directory of the templates.
		 */
		String getDirectory() {
			return config.getString(DIRECTORY);
		}

		/**
		 * @return the extension of the template files in the templates
		 *         directory.
		 */
		String getExtension() {
			return config.getString(EXTENSION);
		}

	}
}
