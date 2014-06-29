package org.http4s.cooldsl.swagger.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** ApiProperty can be put on a Method to allow swagger to understand the json fields datatype and more. */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiModelProperty {
    /** Provide a human readable synopsis of this property */
    String description() default "";

    /**
    * If the values that can be set are restricted, they can be set here. In the form of a comma separated list
    * <code>registered, active, closed</code>.
    *
    * @return the allowable values
    */
    String allowableValues() default "";

    /**
    * specify an optional access value for filtering in a Filter
    * implementation.  This
    * allows you to hide certain parameters if a user doesn't have access to them
    */
    String access() default "";

    /** long description of the property */
    String notes() default "";

    /**
    * Whether or not the property is required, defaults to true.
    *
    * @return true if required, false otherwise
    */
    boolean required() default true;

    /**
    * allows explicitly ordering the property in the model.  Since reflection has no guarantee on
    * ordering, you should specify property order to keep models consistent across different VM implementations and versions.
    */
    int position() default 0;
}
