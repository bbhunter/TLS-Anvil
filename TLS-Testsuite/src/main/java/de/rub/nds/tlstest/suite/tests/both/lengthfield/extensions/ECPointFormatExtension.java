/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.both.lengthfield.extensions;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.anvilcore.coffee4j.model.ModelFromScope;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.scanner.core.probe.result.TestResults;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.extension.ECPointFormatExtensionMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlstest.framework.annotations.KeyExchange;
import de.rub.nds.tlstest.framework.annotations.TlsVersion;
import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.TlsGenericTest;
import org.junit.jupiter.api.Tag;

@Tag("tls12")
@TlsVersion(supported = ProtocolVersion.TLS12)
@KeyExchange(supported = KeyExchangeType.ECDH)
public class ECPointFormatExtension extends TlsGenericTest {

    @AnvilTest(id = "XLF-mgWov7XYiw")
    @ModelFromScope(modelType = "LENGTHFIELD")
    public void pointFormatExtensionLength(WorkflowRunner runner, AnvilTestCase testCase) {
        Config config = context.getConfig().createConfig();
        genericExtensionLengthTest(runner, testCase, config, ECPointFormatExtensionMessage.class);
    }

    @AnvilTest(id = "XLF-XdYDypM7gN")
    @ModelFromScope(modelType = "LENGTHFIELD")
    public void pointFormatExtensionFormatsLength(WorkflowRunner runner, AnvilTestCase testCase) {
        WorkflowTrace workflowTrace = setupLengthFieldTestTls12(runner);
        ECPointFormatExtensionMessage pointFormatExtension =
                getTargetedExtension(ECPointFormatExtensionMessage.class, workflowTrace);
        pointFormatExtension.setPointFormatsLength(Modifiable.sub(1));
        State state = runner.execute(workflowTrace, runner.getPreparedConfig());

        boolean skipsExtensionContent =
                context.getFeatureExtractionResult()
                                .getResult(
                                        TlsAnalyzedProperty.HANDSHAKES_WITH_UNDEFINED_POINT_FORMAT)
                        == TestResults.TRUE;
        if (state.getWorkflowTrace().executedAsPlanned() && skipsExtensionContent) {
            testCase.addAdditionalResultInfo("SUT skips over extension content");
            return;
        }
        validateLengthTest(state, testCase);
    }
}
