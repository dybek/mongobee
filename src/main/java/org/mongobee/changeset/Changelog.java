package org.mongobee.changeset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes containing particular changesets (@{@link org.mongobee.changeset.Changeset})
 * @author lstolowski
 * @since 27/07/2014
 * @see org.mongobee.changeset.Changeset
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Changelog {
  String order() default "";
}