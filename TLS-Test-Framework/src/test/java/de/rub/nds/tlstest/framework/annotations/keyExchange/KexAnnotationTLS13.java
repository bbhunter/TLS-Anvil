package de.rub.nds.tlstest.framework.annotations.keyExchange;

import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsscanner.report.SiteReport;
import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.TestSiteReport;
import de.rub.nds.tlstest.framework.annotations.KeyExchange;
import de.rub.nds.tlstest.framework.annotations.TlsTest;
import de.rub.nds.tlstest.framework.constants.KeyExchangeType;
import de.rub.nds.tlstest.framework.utils.ConditionTest;
import de.rub.nds.tlstest.framework.junitExtensions.KexCondition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.HashSet;

public class KexAnnotationTLS13 {

    @RegisterExtension
    static ConditionTest ext = new ConditionTest(KexCondition.class);

    @BeforeAll
    static void setup() {
        TestContext testContext = new TestContext();
        TestSiteReport report = new TestSiteReport("");

        report.addCipherSuites(new HashSet<CipherSuite>(){
            {
                add(CipherSuite.TLS_AES_256_GCM_SHA384);
            }
        });

        testContext.getConfig().setSiteReport(report);
    }


    @TlsTest
    @KeyExchange(supported = KeyExchangeType.ALL13)
    public void execute_supported() {}

    @TlsTest
    @KeyExchange(supported = KeyExchangeType.ALL12)
    public void not_execute_unsupported() {}

}
