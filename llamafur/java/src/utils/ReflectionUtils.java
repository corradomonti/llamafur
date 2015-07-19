package utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang3.text.WordUtils;

public class ReflectionUtils {
	
	/**
	 * Set a field of an object with a value, also if the field is of primitive type.
	 * 
	 * Apart from this, it mimics behavior from java.reflection.Field.set(Object, Object).
	 */
	public static void setFieldFromObject(Object object, Field field, Object value) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (!field.getType().isPrimitive()) {
			field.set(object, value);
			return;
		}
		
		Class<?> primitiveClass = value.getClass();
		
		if (!primitiveClass.isPrimitive()) {
			primitiveClass = ClassUtils.wrapperToPrimitive(primitiveClass);
			if (primitiveClass == null) {
				throw new IllegalArgumentException(field + " is a primitive field but the value is neither primitive nor wrapper.");
			}
		}
		
		String assignerName = "set" + WordUtils.capitalize(primitiveClass.getSimpleName());
		
		Method assigner = Field.class.getMethod(assignerName, new Class<?>[] {Object.class, primitiveClass});
		assigner.invoke(field, new Object[] {object, value});
		
	}

}
