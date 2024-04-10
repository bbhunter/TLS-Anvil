/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.junitExtensions;

import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Creates a WorkflowRunner object that is passed to a test method that has specified a
 * WorkflowRunner argument.
 */
public class WorkflowRunnerResolver implements ParameterResolver {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean supportsParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(WorkflowRunner.class);
    }

    @Override
    public Object resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (!extensionContext.getTestMethod().isPresent()) {
            return null;
        }

        return new WorkflowRunner(extensionContext);
    }
}
