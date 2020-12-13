package net.lecousin.reactive.data.relational.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a value being generated by the database system.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface GeneratedValue {
	
	enum Strategy {
		/** Use auto increment/serial capability from database. */
		AUTO_INCREMENT,
		/** Use a sequence. Sequence name must be specified. */
		SEQUENCE,
		/** Generate a random UUID. Must be used only with a column of type java.util.UUID. */
		RANDOM_UUID;
	}
	
	/** Strategy to generate the value. */
	Strategy strategy() default Strategy.AUTO_INCREMENT;
	
	/** For SEQUENCE strategy, specifies the name of the sequence to use. */
	String sequence() default ""; 

}
