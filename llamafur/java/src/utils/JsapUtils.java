package utils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.lang.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.StringParser;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;
import com.sun.xml.txw2.IllegalAnnotationException;


public class JsapUtils {
	
	private enum Default {
		NO_DEFAULT;
	}
	
	public static class JsapResultsWithObject<T> {
		JSAPResult jsap;
		T object;
		public JsapResultsWithObject(JSAPResult jsap, T object) {
			this.jsap = jsap; this.object = object;
		}
		public JSAPResult getJsapResult() { return jsap; }
		public T getObject() { return object; }
	}
	
	public static Logger LOGGER = LoggerFactory.getLogger(JsapUtils.class);
	
	public static <T> T constructObject(Class<T> clazz, String[] rawArguments, String mainHelp) throws JSAPException, IOException, IllegalArgumentException, ReflectiveOperationException {
		return constructObject(clazz, rawArguments, mainHelp, new Parameter[] {} ).getObject();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> JsapResultsWithObject<T> constructObject(Class<T> clazz, String[] rawArguments, String mainHelp, Parameter[] otherParameters) throws JSAPException, IOException, IllegalArgumentException, ReflectiveOperationException {
		Constructor<T> constructor = null;
		for (Constructor<?> constr : clazz.getConstructors()) {
			boolean annotationPresent = constr.isAnnotationPresent(CommandLine.class);
			if (annotationPresent) {
				constructor = (Constructor<T>) constr;
				break;
			}
		}
		if (constructor == null) throw new IllegalAccessError("Class " + clazz + " must have a constructor annotated with @CommandLine");
		
		String[] argNames = constructor.getAnnotation(CommandLine.class).argNames();
		if (argNames.length != constructor.getParameterCount()) throw new IllegalAnnotationException("Annotation @CommandLine argNames are out of sync with the constructor arguments.");
		
		boolean[] isSerializedFile = new boolean[argNames.length];
		
		SimpleJSAP jsap = new SimpleJSAP(clazz.getName(), mainHelp, otherParameters);
		int i = 0;
		for (java.lang.reflect.Parameter x : constructor.getParameters()) {
			Parameter option;
			if (x.getType().equals(boolean.class)) {
				isSerializedFile[i] = false;
				option = new Switch(argNames[i], JSAP.NO_SHORTFLAG, argNames[i], 
						 "Set the value of " + argNames[i] + " for " + clazz.getSimpleName() + " as true.");
			} else {
				StringParser parser;
				String help;
				try {
					parser = getParserFor(x.getType());
					isSerializedFile[i] = false;
					help = "The " + x.getType().getSimpleName() + " value of " + argNames[i];
				} catch (NoJSAPParserForThisTypeException e) {
					parser = JSAP.STRING_PARSER;
					isSerializedFile[i] = true;
					help = "A serialized " + x.getType().getSimpleName() + " file to initialize " + argNames[i];
				}
				option = new UnflaggedOption( argNames[i], parser, JSAP.REQUIRED, help);
			}
					
				
			jsap.registerParameter(option);
			
			i++;
		}
		
		
		
		JSAPResult args = jsap.parse( rawArguments );
		if ( jsap.messagePrinted() ) System.exit( 1 );
		
		LOGGER.info("Initializing...");
		Object[] arguments = new Object[argNames.length];
		i = 0;
		for (String argName : argNames) {
			if (isSerializedFile[i])
				arguments[i] = SerializationUtils.read(args.getString(argName));
			else
				arguments[i] = args.getObject(argName);
			i++;
		}
		T object = constructor.newInstance(arguments);
		LOGGER.info("Ready.");
		
		return new JsapResultsWithObject<T>(args, object);
	}
	
	public static void addParametersFromClass(JSAP jsap, Class<?> clazz, Object objectWithDefaults) throws JSAPException, ReflectiveOperationException {
		addParametersFromClass(jsap, clazz, objectWithDefaults, new String[] {});
	}
	
	public static void addParametersFromClass(JSAP jsap, Class<?> clazz, Object objectWithDefaults, String[] excluded) throws JSAPException, ReflectiveOperationException {
		
		boolean allRequired = (objectWithDefaults == Default.NO_DEFAULT);
		
		for (Field field : clazz.getFields() ) {
				if (Modifier.isPublic(field.getModifiers())) {
					
					String defaultValue = JSAP.NO_DEFAULT;
	
					if (!allRequired) {
						Object defaultValueObj = field.get(objectWithDefaults);
						if (defaultValueObj == null)
							defaultValue = JSAP.NO_DEFAULT;
						else
							defaultValue = defaultValueObj.toString();
					}
					
					String id = field.getName().toLowerCase();
					boolean isToExclude = false;
					for (String exclude : excluded)
						if (exclude.equalsIgnoreCase(id)) {
							isToExclude = true;
							break;
						}
					
					if (!isToExclude)
						jsap.registerParameter(new FlaggedOption(
								id, //id
								getParserFor(field.getType()), // string parser
								defaultValue,
								allRequired,
								JSAP.NO_SHORTFLAG,
								id
								//, getHelpFor(field)
						));
					
					
				}
		}
	}
	
	private static void applyParameters(JSAPResult args, Object o, Class<?> clazz) throws ReflectiveOperationException {
		for (Field field : clazz.getFields() ) {
			if (Modifier.isPublic(field.getModifiers())) {
				String fieldName = field.getName().toLowerCase();
				if (args.contains(fieldName)) {
					Object parameterValue = args.getObject(fieldName);
					ReflectionUtils.setFieldFromObject(o, field, parameterValue);
	
					LOGGER.info("Setting " + fieldName + " to '" + parameterValue + "'.");
				} else {
					LOGGER.warn("The field " + fieldName + " has not been set by command line arguments.");
				}
			}
		}
	}	
	public static void applyStaticParameters(JSAPResult args, Class<?> clazz) throws ReflectiveOperationException {
		applyParameters(args, null, clazz);
	}
	public static void applyParameters(JSAPResult args, Object object) throws ReflectiveOperationException {
		applyParameters(args, object, object.getClass());
	}
	
	@SuppressWarnings("unused")
	private static String getHelpFor(Field f) {
		return "Set the field " + f.getName() + " of " + f.getDeclaringClass() + " (see javadoc for details).";
	}
	
	private static StringParser getParserFor(Class<?> fieldType) {
		String typeName;
		if (fieldType.isPrimitive()) {
			typeName = ClassUtils.primitiveToWrapper(fieldType).getSimpleName();
		} else {
			typeName = fieldType.getSimpleName();
		}
		try {
			Class<?> parserClass = Class.forName(
					"com.martiansoftware.jsap.stringparsers."
					+ typeName + "StringParser"
				);
			
			 Method getParser = parserClass.getMethod("getParser", new Class<?>[] {});
			 
			 return (StringParser) getParser.invoke(null, new Object[] {});
			 
		} catch (ClassNotFoundException e) {
			throw new NoJSAPParserForThisTypeException("JSAP parameters can not be made out of type " + typeName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("serial")
	private static class NoJSAPParserForThisTypeException extends RuntimeException {
		NoJSAPParserForThisTypeException(String s) { super(s); }
	}
}
