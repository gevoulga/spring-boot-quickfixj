package ch.voulgarakis.spring.boot.starter.quickfixj;

import ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure.QuickFixJAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(QuickFixJAutoConfiguration.class)
@AutoConfigurationPackage
public @interface EnableQuickFixJ {

}
