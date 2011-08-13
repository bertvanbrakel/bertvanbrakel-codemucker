package com.bertvanbrakel.test.bean.random;

import static com.bertvanbrakel.test.bean.ClassUtils.invokeCtorWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.bertvanbrakel.test.bean.BeanDefinition;
import com.bertvanbrakel.test.bean.BeanException;
import com.bertvanbrakel.test.bean.PropertiesExtractor;
import com.bertvanbrakel.test.bean.Property;

public class BeanRandom implements RandomDataProvider {

	private final PropertiesExtractor extractor = new PropertiesExtractor();

	private static Map<Class<?>, RandomDataProvider<?>> builtInProviders = new HashMap<Class<?>, RandomDataProvider<?>>();

	private final PrimitiveProvider primitiveProvider = new PrimitiveProvider();
	private final CollectionProvider collectionProvider = new CollectionProvider(this);
	private final EnumProvider enumProvider = new EnumProvider();

	private Stack<String> parentPropertes = new Stack<String>();

	// to prevent infinite recursion
	private Stack<Class<?>> parentBeansTypesCreated = new Stack<Class<?>>();

	private String parentPropertyPath;

	public BeanRandom() {
		extractor.setOptions(new RandomOptions());
	}

	public <T> T populate(Class<T> beanClass) {
		BeanDefinition def = extractor.extractBeanDefWithCtor(beanClass);
		if (def.getCtor() == null) {
			throw new BeanException(
			        "Could not find a valid ctor for bean class %s. Are you sure your bean ctor is public (or if you have no ctor that your bean is public) and the bean is not a non static inner class?",
			        beanClass.getName());

		}
		T bean = invokeCtorWithRandomArgs((Constructor<T>) def.getCtor());
		populatePropertiesWithRandomValues(bean);
		return bean;
	}

	private void populatePropertiesWithRandomValues(Object bean) {
		BeanDefinition def = extractor.extractBeanDef(bean.getClass());
		for (Property p : def.getProperties()) {
			if (!p.isIgnore()) {
				if (isGenerateRandomPropertyValue(bean.getClass(), p.getName(), p.getType())) {
					populatePropertyWithRandomValue(p, bean);
				}
			}
		}
	}

	private void populatePropertyWithRandomValue(Property p, Object bean) {
		if (p.getWrite() != null) {
			Method setter = p.getWrite();
			Object propertyValue = getRandom(null, p.getName(), p.getType(), p.getGenericType());
			// TODO:option to ignore errors?
			try {
				setter.invoke(bean, new Object[] { propertyValue });
			} catch (IllegalArgumentException e) {
				throw new BeanException("Error invoking setter %s on property '%s' on class %s",
				        setter.toGenericString(), p.getName(), bean.getClass().getName());
			} catch (IllegalAccessException e) {
				throw new BeanException("Error invoking setter %s on property '%s' on class %s",
				        setter.toGenericString(), p.getName(), bean.getClass().getName());
			} catch (InvocationTargetException e) {
				throw new BeanException("Error invoking setter %s on property '%s' on class %s",
				        setter.toGenericString(), p.getName(), bean.getClass().getName());
			}
		}
	}

	private boolean isGenerateRandomPropertyValue(Class<?> beanClass, String propertyName, Class<?> propertyType) {
		if (parentPropertyPath != null) {
			String fullPath = parentPropertyPath + propertyName;
			if (getOptions().getIgnoreProperties().contains(fullPath)) {
				return false;
			}
		}

		return true;
	}

	private void pushBeanProperty(String propertyName, Class<?> propertyType) {
		parentBeansTypesCreated.add(propertyType);
		parentPropertes.push(propertyName);
		parentPropertyPath = joinParentProperties();
	}

	private void popBeanProperty() {
		parentBeansTypesCreated.pop();
		parentPropertes.pop();
		parentPropertyPath = joinParentProperties();
	}

	private String joinParentProperties() {
		if (parentPropertes.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (String name : parentPropertes) {
				sb.append(name);
				sb.append('.');
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	protected <T> T invokeCtorWithRandomArgs(Constructor<T> ctor) {
		int len = ctor.getParameterTypes().length;
		Object[] args = new Object[len];
		for (int i = 0; i < len; i++) {
			args[i] = getRandom(ctor.getDeclaringClass(), null, ctor.getParameterTypes()[i], ctor.getGenericParameterTypes()[i]);
		}
		T bean = invokeCtorWith(ctor, args);
		return bean;
	}
	
	@Override
	public Object getRandom(Class beanClass, String propertyName, Class propertyType, Type genericType) {
		RandomDataProvider<?> provider = getOptions().getProvider(propertyType);
		if (provider == null) {
			provider = builtInProviders.get(propertyType);
			if (provider == null) {
				if (propertyType.isArray() || Collection.class.isAssignableFrom(propertyType)) {
					provider = collectionProvider;
				} else if (propertyType.isEnum()) {
					provider = enumProvider;
				} else if (primitiveProvider.supportsType(propertyType)) {
					provider = primitiveProvider;
				}
			}
		}
		if (provider == null) {
			// lets create the bean
			if (isGenerateBeanPropertyOfType(beanClass, propertyName, propertyType, genericType)) {
				if (parentBeansTypesCreated.contains(propertyType)) {
					if (getOptions().isFailOnRecursiveBeanCreation()) {
						throw new BeanException("Recursive bean creation for type %s for property %s",
						        propertyType.getName(), propertyName);
					} else {
						return null;
					}
				}
				try {
					pushBeanProperty(propertyName, propertyType);
					return populate(propertyType);
				} finally {
					popBeanProperty();
				}
			} else {
				if (getOptions().isFailOnNonSupportedPropertyType()) {
					throw new BeanException("no provider for type %s for property '%s'", propertyType, propertyName);
				}
				return null;
			}
		}
		return provider.getRandom(beanClass, propertyName, propertyType, genericType);
	}

	private boolean isGenerateBeanPropertyOfType(Object bean, String propertyName, Class<?> type, Type genericType) {
		return getOptions().isGeneratePropertyType(bean, propertyName, type, genericType);
	}

	public RandomOptions getOptions() {
		return (RandomOptions) extractor.getOptions();
	}

	public void setOptions(RandomOptions options) {
		extractor.setOptions(options);
	}
}
