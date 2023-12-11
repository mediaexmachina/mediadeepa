/*
 * This file is part of mediadeepa.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2023
 *
 */
package media.mexm.mediadeepa;

import static java.util.function.Predicate.not;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.ClassUtils;

public class ConfigurationCrawler {

	private final Object instance;
	private final Class<?> configClass;
	private final String rootPrefix;

	public ConfigurationCrawler(final Object instance) {
		this.instance = Objects.requireNonNull(instance, "\"instance\" can't to be null");
		configClass = ClassUtils.getUserClass(instance.getClass());
		rootPrefix = Optional.ofNullable(configClass.getAnnotation(ConfigurationProperties.class))
				.map(ConfigurationProperties::prefix)
				.flatMap(Optional::ofNullable)
				.orElse("");
	}

	public String getRootPrefix() {
		return rootPrefix;
	}

	public List<ConfigurationEntry> parse() {
		return methodExtractor(Optional.empty(), configClass.getMethods(), instance)
				.flatMap(ConfigurationEntryImpl::getSubEntries)
				.sorted((l, r) -> l.getKey().compareTo(r.getKey()))
				.map(ce -> (ConfigurationEntry) ce)
				.toList();
	}

	private static Stream<ConfigurationEntryImpl> methodExtractor(final Optional<ConfigurationEntryImpl> parent,
																  final Method[] methods,
																  final Object enclosingInstance) {
		return Stream.of(methods)
				.filter(m -> m.getModifiers() == Modifier.PUBLIC)
				.filter(m -> m.getParameterCount() == 0)
				.filter(m -> m.getName().startsWith("get") || m.getName().startsWith("is"))
				.filter(m -> m.getReturnType() != null)
				.flatMap(m -> methodInvoker(m, enclosingInstance)
						.map(defaultValue -> new ConfigurationEntryImpl(parent, m, defaultValue))
						.stream());
	}

	private static Optional<Object> methodInvoker(final Method method, final Object enclosingInstance) {
		try {
			return Optional.ofNullable(method.invoke(enclosingInstance))
					.filter(not(enclosingInstance::equals));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalCallerException("Can't extract default value for "
											 + enclosingInstance.getClass().getName()
											 + "/" + method.getName(), e);
		}
	}

	public interface ConfigurationEntry {
		String getKey();

		default String getFullKey(final String rootPrefix) {
			if (rootPrefix == null || rootPrefix.isEmpty()) {
				return getKey();
			}
			return rootPrefix + "." + getKey();
		}

		String getType();

		String getDefaultValue();
	}

	private record ConfigurationEntryImpl(Optional<ConfigurationEntryImpl> parent,
										  Method method,
										  Object defaultValue) implements ConfigurationEntry {

		boolean isDirectyAssignable() {
			final var returnType = method.getReturnType();
			return returnType.isEnum()
				   || returnType.isAssignableFrom(String.class)
				   || returnType.isPrimitive()
				   || returnType.isAssignableFrom(Number.class)
				   || returnType.isAssignableFrom(Boolean.class)
				   || returnType.isAssignableFrom(Duration.class);
		}

		Stream<ConfigurationEntryImpl> getSubEntries() {
			if (isDirectyAssignable()) {
				return Stream.of(this);
			}
			return methodExtractor(Optional.ofNullable(this), method.getReturnType().getMethods(), defaultValue)
					.flatMap(ConfigurationEntryImpl::getSubEntries);
		}

		@Override
		public String getKey() {
			var key = method.getName();
			if (key.startsWith("get")) {
				key = key.substring(3);
			} else if (key.startsWith("is")) {
				key = key.substring(2);
			}

			final var finalKey = new StringBuilder();
			final var sz = key.length();
			finalKey.append(String.valueOf(key.charAt(0)).toLowerCase());
			for (var i = 1; i < sz; i++) {
				if (Character.isUpperCase(key.charAt(i))) {
					finalKey.append("-");
					finalKey.append(String.valueOf(key.charAt(i)).toLowerCase());
				} else {
					finalKey.append(key.charAt(i));
				}
			}

			return parent
					.map(ConfigurationEntryImpl::getKey)
					.map(fk -> fk + "." + finalKey.toString())
					.orElse(finalKey.toString());
		}

		@Override
		public String getType() {
			return method.getReturnType().getSimpleName();
		}

		@Override
		public String getDefaultValue() {
			return String.valueOf(defaultValue);
		}

	}

}
