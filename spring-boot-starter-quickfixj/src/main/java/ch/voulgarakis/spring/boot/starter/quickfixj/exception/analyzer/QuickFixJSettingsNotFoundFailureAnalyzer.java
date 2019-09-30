package ch.voulgarakis.spring.boot.starter.quickfixj.exception.analyzer;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJSettingsNotFoundException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class QuickFixJSettingsNotFoundFailureAnalyzer extends AbstractFailureAnalyzer<QuickFixJSettingsNotFoundException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, QuickFixJSettingsNotFoundException cause) {
        return new FailureAnalysis(cause.getMessage(),
                "Provide a QuickFixJ settings file.",
                cause);
    }

}
