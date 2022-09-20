package task;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(Param.Params.class)
public @interface Param {
	String name();
	task.Descriptor.Param.Type type();
	String preset();
	String description();
	
	@Retention(RUNTIME)
	@Target(TYPE) 
	@interface Params {
		Param[] value();
	}	
}