package task;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(Role.Roles.class)
public @interface Role {
	String value();
	
	@Retention(RUNTIME)
	@Target(TYPE)
    @interface Roles {
		Role[] value();
    }
}