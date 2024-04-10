/**
 * TLS-Testsuite - A testsuite for the TLS protocol
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.suite.tests.client.tls13.rfc8446;

import de.rub.nds.anvilcore.annotation.ClientTest;
import de.rub.nds.anvilcore.annotation.NonCombinatorialAnvilTest;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlstest.framework.testClasses.Tls13Test;
import java.util.Arrays;

@ClientTest
public class Cookie extends Tls13Test {

    @NonCombinatorialAnvilTest(id = "8446-C9aFBzrCbX")
    public void clientHelloContainsCookieExtension() {
        int size =
                (int)
                        context.getReceivedClientHelloMessage().getExtensions().stream()
                                .filter(
                                        i ->
                                                Arrays.equals(
                                                        ExtensionType.COOKIE.getValue(),
                                                        i.getExtensionType().getValue()))
                                .count();
        if (size > 0) {
            throw new AssertionError("Regular ClientHello contains Cookie extension");
        }
    }
}
