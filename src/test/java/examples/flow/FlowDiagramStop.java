package examples.flow;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Stop is no diagram element but a marker to stop the processing of a flow. This is required to avoid cycles in the
 * diagram when generic methods are used twice.
 */
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE_USE, ElementType.LOCAL_VARIABLE })
@Retention(RetentionPolicy.RUNTIME)
public @interface FlowDiagramStop {

}
