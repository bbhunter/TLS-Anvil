/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.both.lengthfield.extensions;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.anvilcore.annotation.ServerTest;
import de.rub.nds.anvilcore.coffee4j.model.ModelFromScope;
import de.rub.nds.anvilcore.teststate.AnvilTestCase;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.extension.SignatureAndHashAlgorithmsExtensionMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlstest.framework.annotations.KeyExchange;
import de.rub.nds.tlstest.framework.annotations.TlsVersion;
import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.execution.WorkflowRunner;
import de.rub.nds.tlstest.framework.testClasses.TlsGenericTest;
import org.junit.jupiter.api.Tag;

@ServerTest
public class SignatureAndHashAlgorithmsExtension extends TlsGenericTest {

    @TlsVersion(supported = ProtocolVersion.TLS12)
    @KeyExchange(supported = KeyExchangeType.ALL12)
    @AnvilTest(id = "XLF-Dtq2iEmPmd")
    @ModelFromScope(modelType = "LENGTHFIELD")
    public void signatureAndHashAlgorithmsExtensionLengthTLS12(
            AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = context.getConfig().createConfig();
        config.setAddSignatureAndHashAlgorithmsExtension(true);
        genericExtensionLengthTest(
                runner, testCase, config, SignatureAndHashAlgorithmsExtensionMessage.class);
    }

    @Tag("tls13")
    @TlsVersion(supported = ProtocolVersion.TLS13)
    @KeyExchange(supported = KeyExchangeType.ALL13)
    @AnvilTest(id = "XLF-s6s3mWStow")
    @ModelFromScope(modelType = "LENGTHFIELD")
    public void signatureAndHashAlgorithmsExtensionLengthTLS13(
            AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = context.getConfig().createTls13Config();
        config.setAddSignatureAndHashAlgorithmsExtension(true);
        genericExtensionLengthTest(
                runner, testCase, config, SignatureAndHashAlgorithmsExtensionMessage.class);
    }

    @Tag("tls12")
    @TlsVersion(supported = ProtocolVersion.TLS12)
    @KeyExchange(supported = KeyExchangeType.ALL12)
    @AnvilTest(id = "XLF-x666dC8D1Z")
    @ModelFromScope(modelType = "LENGTHFIELD")
    public void signatureAndHashAlgorithmsListLengthTLS12(
            AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = context.getConfig().createConfig();
        signatureAndHashAlgorithmsListLengthTest(config, runner, testCase);
    }

    @Tag("tls13")
    @TlsVersion(supported = ProtocolVersion.TLS13)
    @KeyExchange(supported = KeyExchangeType.ALL13)
    @AnvilTest(id = "XLF-Qm9jhF6Pn8")
    @ModelFromScope(modelType = "LENGTHFIELD")
    public void signatureAndHashAlgorithmsListLengthTLS13(
            AnvilTestCase testCase, WorkflowRunner runner) {
        Config config = context.getConfig().createTls13Config();
        signatureAndHashAlgorithmsListLengthTest(config, runner, testCase);
    }

    private void signatureAndHashAlgorithmsListLengthTest(
            Config versionBasedConfig, WorkflowRunner runner, AnvilTestCase testCase) {
        versionBasedConfig.setAddSignatureAndHashAlgorithmsExtension(true);
        WorkflowTrace workflowTrace = setupLengthFieldTestForConfig(versionBasedConfig, runner);
        SignatureAndHashAlgorithmsExtensionMessage sigAndHashAlgorithmsExtension =
                getTargetedExtension(
                        SignatureAndHashAlgorithmsExtensionMessage.class, workflowTrace);
        sigAndHashAlgorithmsExtension.setSignatureAndHashAlgorithmsLength(Modifiable.sub(1));
        State state = runner.execute(workflowTrace, runner.getPreparedConfig());
        validateLengthTest(state, testCase);
    }
}
