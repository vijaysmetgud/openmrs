/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.util;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * This class has convenience methods to find the fields on a class and superclass as well as
 * methods to check the class type of members in a collection
 */
public class Reflect {
	
	
	private Class parametrizedClass;
	
	/**
	 * @param parametrizedClass Class
	 * <strong>Should</strong> throw exception when null is passed
	 */
	public Reflect(Class parametrizedClass) {
		
		if (parametrizedClass == null) {
			throw new NullPointerException("Parametrized class cannot be null");
		}
		this.parametrizedClass = parametrizedClass;
	}
	
	/**
	 * @param fieldClass
	 * @return true if, given fieldClass is Collection otherwise returns false
	 * <strong>Should</strong> return true if given fieldClass is Collection class
	 * <strong>Should</strong> return false if given fieldClass is not a Collection class
	 */
	public static boolean isCollection(Class<?> fieldClass) {
		return Collection.class.isAssignableFrom(fieldClass);
	}
	
	/**
	 * @param object Object
	 * @return true if, given object is Collection otherwise returns false
	 * <strong>Should</strong> return true if given object is Collection class
	 * <strong>Should</strong> return false if given object is not a Collection
	 */
	public static boolean isCollection(Object object) {
		return isCollection(object.getClass());
	}
	
	/**
	 * This method return all the fields (including private) from the given class and its super
	 * classes.
	 * 
	 * @param fieldClass Class
	 * @return List&lt;Field&gt;
	 * <strong>Should</strong> return all fields include private and super classes
	 */
	public static List<Field> getAllFields(Class<?> fieldClass) {
		List<Field> fields = new ArrayList<>();
		while (fieldClass != null) {
			Field[] declaredFields = fieldClass.getDeclaredFields();
			for (Field field : declaredFields) {
				field.setAccessible(true);
				fields.add(field);
			}
			fieldClass = fieldClass.getSuperclass();
		}
		return fields;
	}
	
	/**
	 * This method returns true if the given annotation is present on the given field.
	 * 
	 * @param fieldClass
	 * @param fieldName
	 * @param annotation
	 * @return true if the given annotation is present
	 */
	public static boolean isAnnotationPresent(Class<?> fieldClass, String fieldName, Class<? extends Annotation> annotation) {
		try {
			Field field = fieldClass.getDeclaredField(fieldName);
			return field.isAnnotationPresent(annotation);
		} catch (NoSuchFieldException e) {
			return false; 
		}
	}
	
	/**
	 * @param subClass Class
	 * @return true if, given subClass is accessible from the parameterized class
	 * <strong>Should</strong> return true if given subClass is accessible from given parameterized class
	 * <strong>Should</strong> return false if given subClass is not accessible from given parameterized class
	 */
	@SuppressWarnings("unchecked")
	public boolean isSuperClass(Class subClass) {
		return parametrizedClass.isAssignableFrom(subClass);
	}
	
	/**
	 * @param t
	 * @return true if given type is a subclass, or a generic type bounded by a subclass of the
	 *         parameterized class
	 * <strong>Should</strong> return true for a generic whose bound is a subclass
	 * <strong>Should</strong> return false for a generic whose bound is not a subclass
	 */
	public boolean isSuperClass(Type t) {
		if (t instanceof TypeVariable<?>) {
			TypeVariable<?> typeVar = (TypeVariable<?>) t;
			if (typeVar.getBounds() == null || typeVar.getBounds().length == 0) {
				return parametrizedClass.equals(Object.class);
			}
			for (Type typeBound : typeVar.getBounds()) {
				if (isSuperClass(typeBound)) {
					return true;
				}
			}
			return false;
		} else if (t instanceof Class<?>) {
			return isSuperClass((Class<?>) t);
		} else {
			throw new IllegalArgumentException("Don't know how to handle: " + t.getClass());
		}
	}
	
	/**
	 * @param object Object
	 * @return true if, given object is accessible from the parameterized class
	 * <strong>Should</strong> return true if given object is accessible from given parameterized class
	 * <strong>Should</strong> return false if given object is not accessible from given parameterized class
	 */
	public boolean isSuperClass(Object object) {
		return isSuperClass(object.getClass());
	}
	
	/**
	 * This method validate the given field is Collection and the elements should be of
	 * parameterized type
	 * 
	 * @param field Field
	 * @return boolean
	 * <strong>Should</strong> return true if given field is Collection and its element type is given parameterized
	 *         class type
	 * <strong>Should</strong> return false if given field is not a Collection
	 * <strong>Should</strong> return false if given field is Collection and element type is other than given
	 *         parameterized class type
	 */
	@SuppressWarnings("unchecked")
	public boolean isCollectionField(Field field) {
		if (isCollection(field.getType())) {
			try {
				ParameterizedType type = (ParameterizedType) field.getGenericType();
				if (type.getActualTypeArguments()[0] instanceof Class) {
					return (parametrizedClass.isAssignableFrom((Class) type.getActualTypeArguments()[0]));
				} else if (type.getActualTypeArguments()[0] instanceof TypeVariable) {
					return isSuperClass(type.getActualTypeArguments()[0]);
				}
			}
			catch (ClassCastException e) {
				// Do nothing.  If this exception is thrown, then field is not a Collection of OpenmrsObjects
			}
		}
		return false;
	}
	
	/**
	 * This method return all the fields (including private) until the given parameterized class
	 * 
	 * @param subClass Class
	 * @return List&lt;Field&gt;
	 * <strong>Should</strong> return only the sub class fields of given parameterized class
	 */
	public List<Field> getInheritedFields(Class<?> subClass) {
		
		List<Field> allFields = getAllFields(subClass);
		allFields.removeIf(field -> !hasField(field));
		
		return allFields;
	}
	
	/**
	 * @param field
	 * @return true if, given field is declared in parameterized class or its sub classes
	 */
	public boolean hasField(Field field) {
		return isSuperClass(field.getDeclaringClass());
	}
	
}
