package org.apache.edgent.connectors.kafka.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In order to prevent the need to implement multiple versions of kafka-adapters.
 * The {@link KafkaVersion} annotation allows specifying a minimum and maximum version
 * of Kafka that supports a given functionality. This information currently can only
 * be provided on a class/type (includes all methods) or individual method level.
 *
 * This information is then used by an aspect that wraps access to version constrained
 * methods and throws meaningful exceptions when being used without satisfying the
 * version criteria.
 */
// We need the information to be available at runtime.
@Retention(RetentionPolicy.RUNTIME)
// We will only annotate methods or classes with this version check.
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface KafkaVersion {

    String fromVersion();

    String toVersion();

}
