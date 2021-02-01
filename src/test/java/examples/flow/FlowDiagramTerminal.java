package examples.flow;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Similar to an action but indicates the begin or ending of a flow. When a generation is triggered, a diagram is generated
 * for every method containing a Terminal annotation in the same class.
 */
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE_USE, ElementType.LOCAL_VARIABLE })
@Retention(RetentionPolicy.RUNTIME)
public @interface FlowDiagramTerminal {

    /**
     * @return optional, name of the diagram element or the method name if blank
     */
    String value() default "";
}
