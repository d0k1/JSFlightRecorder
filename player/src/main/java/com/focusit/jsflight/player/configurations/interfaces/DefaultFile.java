package com.focusit.jsflight.player.configurations.interfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Gallyam Biktashev on 28.10.16.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultFile {
    String value();

    String rootDirectory() default "defaults";
}
