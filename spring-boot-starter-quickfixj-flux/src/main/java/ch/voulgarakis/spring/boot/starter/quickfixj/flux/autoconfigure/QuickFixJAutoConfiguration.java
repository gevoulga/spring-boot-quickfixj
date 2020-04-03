package ch.voulgarakis.spring.boot.starter.quickfixj.flux.autoconfigure;


import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure.QuickFixJBootProperties;
import ch.voulgarakis.spring.boot.starter.quickfixj.flux.logging.ReactiveMdcContextConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage
@EnableConfigurationProperties(QuickFixJBootProperties.class)
@ConditionalOnBean(annotation = EnableQuickFixJ.class)
//@ConditionalOnBean(Application.class)
//@Conditional(QuickFixJAutoConfigurationConditional.class)
//@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class QuickFixJAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "logging.rxMDC", havingValue = "true")
    public ReactiveMdcContextConfiguration reactiveMdcContextConfiguration() {
        return new ReactiveMdcContextConfiguration();
    }
}