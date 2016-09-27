package com.github.andriykuba.play.handlebars;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import play.data.Form;
import play.data.validation.ValidationError;

/**
 * It offers set of methods to simplify work with handlebars form the play
 * framework.
 *
 */
public class HandlebarsTools {
	private final static String MESSAGE_KEY_ERROR = "error";
	private final static String MESSAGE_KEY_FORMS = "forms";

	/**
	 * Takes the errors from the {@link play.data.Form} and put them as keys in
	 * to the map.
	 * 
	 * The generated keys looks like
	 * "forms.{formMessageId}.{formFieldId}.{error.message.key}". For example
	 * "forms.login.login.error.required"
	 * 
	 * The global error does not have the {formFieldId}. For example
	 * "forms.login.error.unavailable"
	 * 
	 * Only one error per time returned, no array of errors like in the original
	 * {@link play.data.Form#errors()}
	 * 
	 * @param form
	 *            The form to process
	 * @param formMessageId
	 *            ID for the messages
	 * @return Convert error messages to the map of message IDs
	 */
	public static Map<String, Object> getErrorsAsMap(Form<?> form, String formMessageId) {
		final Map<String, Object> formErrors = new HashMap<>();

		final Map<String, List<ValidationError>> errors = form.errors();
		for (Map.Entry<String, List<ValidationError>> error : errors.entrySet()) {

			// Only one error is out by time, no mess with a set of errors.
			final List<ValidationError> validationErrors = error.getValue();
			if (!validationErrors.isEmpty()) {
				ValidationError validationError = validationErrors.get(0);
				final String key = validationError.key();
				if (!StringUtils.isEmpty(key)) {
					// Field dependent error.
					final String messageKey = new StringBuilder().append(MESSAGE_KEY_FORMS).append(".")
							.append(formMessageId).append(".").append(key).append(".").append(validationError.message())
							.toString();

					HashMap<String, Object> fieldErrors = new HashMap<>();
					fieldErrors.put(MESSAGE_KEY_ERROR, messageKey);

					formErrors.put(key, fieldErrors);
				} else {
					// Global error.
					final String messageKey = new StringBuilder().append(MESSAGE_KEY_FORMS).append(".")
							.append(formMessageId).append(".").append(validationError.message()).toString();
					formErrors.put(MESSAGE_KEY_ERROR, messageKey);
				}
			}
		}

		return formErrors;
	}

	/**
	 * Puts the {@code object} into the {@code base}.
	 * 
	 * The key is the {@code keyPath}. It breaks the string by dots and creates
	 * an enclosed map for an every element.
	 * 
	 * All maps must have the key as String.
	 * 
	 * Error will be generated in the case of the value is already exist in the
	 * {@code keyPath}
	 * 
	 * 
	 * @param base
	 *            The base map, i.e. root
	 * @param keyPath
	 *            Path to put, like the path in JSON object
	 * @param object
	 *            Object to put
	 */
	@SuppressWarnings("unchecked")
	public static void put(final Map<String, Object> base, final String keyPath, final Object object) {
		// Need to be optimized

		if (object == null) {
			// No object, no operations.
			return;
		}

		if (StringUtils.isEmpty(keyPath) && object instanceof Map) {
			// Do the map merging
			Map<String, Object> mapToPut = (Map<String, Object>) object;
			for (Map.Entry<String, Object> entry : mapToPut.entrySet()) {
				// The map value could be an map as well, so we go in to recursion.
				put(base, entry.getKey(), entry.getValue());
			}
		} else {
			// Put the object in to the map.
			if (StringUtils.isEmpty(keyPath)) {
				// It's not map merge, so we need to have a key.
				throw new UnsupportedOperationException("Object must have a key to be putted in.");
			} else {
				String rootKey = StringUtils.substringBefore(keyPath, ".");
				String tailPath = StringUtils.substringAfter(keyPath, ".");
				Object value = base.get(rootKey);

				if (StringUtils.isEmpty(tailPath)) {
					// No more keys in the path.
					if (value == null) {
						// Just put an object in to the map with the given key.
						base.put(rootKey, object);
					} else if (value instanceof Map) {
						put((Map<String, Object>) value, null, object);
					} else {
						// The value is already present.
						throw new UnsupportedOperationException("The key \"" + rootKey + "\" is already assigned");
					}
				} else {
					// There are more keys in the path.
					if (value == null) {
						// Just string path into a path of maps.
						String[] pathElements = tailPath.split("\\.");
						Map<String, Object> tail = new HashMap<>();
						tail.put(pathElements[pathElements.length - 1], object);
						for (int i = pathElements.length - 2; i >= 0; i--) {
							// Convert path string in to the map branch
							HashMap<String, Object> newTail = new HashMap<>();
							newTail.put(pathElements[i], tail);
							tail = newTail;
						}
						base.put(rootKey, tail);

					} else if (value instanceof Map) {
						// assume that every map in structure is Map<String, Object>
						put((Map<String, Object>) value, tailPath, object);
					} else {
						throw new UnsupportedOperationException(
								"The key \"" + rootKey + "\" is already assigned. Not a map");
					}
				}
			}
		}
	}
}
