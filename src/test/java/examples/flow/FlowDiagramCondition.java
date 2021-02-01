package examples.flow;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * With a condition, the execution flow is divided in two or more branches.
 */
@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE_USE, ElementType.LOCAL_VARIABLE })
@Retention(RetentionPolicy.RUNTIME)
public @interface FlowDiagramCondition {

    /**
     * @return name of the condition element
     */
    String value() default "";

    /**
     * @return name of the branch/arrow
     */
    String branch() default "";

    /**
     * The alternative branch contains all actions after the main branch is finished. E.g. if the main branch is a dead end.
     *
     * @return optional, name of the alternative branch/arrow
     */
    String alternativeBranch() default "";
}
