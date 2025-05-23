
package com.naver.playlist.domain.generator;

import java.lang.annotation.*;
import org.hibernate.annotations.IdGeneratorType;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@IdGeneratorType(IDGenerator.class)
public @interface SnowflakeId {}