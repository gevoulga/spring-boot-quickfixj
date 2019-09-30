package ch.voulgarakis.spring.boot.starter.quickfixj.exception.analyzer;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class QuickFixJConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<QuickFixJConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, QuickFixJConfigurationException cause) {
        return new FailureAnalysis(cause.getMessage(),
                "Please configure your QuickFixJ settings as per the documentation: https://www.quickfixj.org/usermanual/2.1.0/usage/configuration.html",
                cause);
    }

}
