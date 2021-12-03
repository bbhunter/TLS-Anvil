/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * Copyright 2020 Ruhr University Bochum and
 * TÜV Informationstechnik GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.rub.nds.modifiablevariable.util.Modifiable;
import de.rub.nds.tlsattacker.core.certificate.CertificateByteChooser;
import de.rub.nds.tlsattacker.core.certificate.CertificateKeyPair;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.connection.OutboundConnection;
import de.rub.nds.tlsattacker.core.constants.AlgorithmResolver;
import de.rub.nds.tlsattacker.core.constants.CertificateKeyType;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.HashAlgorithm;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.constants.SignatureAlgorithm;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;
import de.rub.nds.tlsattacker.core.protocol.message.ApplicationMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ChangeCipherSpecMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.EllipticCurvesExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.KeyShareExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.SignatureAndHashAlgorithmsExtensionMessage;
import de.rub.nds.tlsattacker.core.protocol.message.extension.SupportedVersionsExtensionMessage;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.record.layer.RecordLayerFactory;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.action.executor.ActionOption;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsattacker.core.workflow.task.StateExecutionTask;
import de.rub.nds.tlsattacker.core.workflow.task.TlsTask;
import de.rub.nds.tlsattacker.transport.Connection;
import de.rub.nds.tlsattacker.transport.tcp.ServerTcpTransportHandler;
import de.rub.nds.tlsscanner.serverscanner.TlsScanner;
import de.rub.nds.tlsscanner.serverscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.TestSiteReport;
import de.rub.nds.tlstest.framework.config.TestConfig;
import de.rub.nds.tlstest.framework.constants.TestEndpointType;
import de.rub.nds.tlstest.framework.model.ParameterExtensionManager;
import de.rub.nds.tlstest.framework.parameterExtensions.configurationOptionsExtension.ConfigurationOptionsExtension;
import de.rub.nds.tlstest.framework.reporting.ExecutionListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import org.junit.platform.launcher.listeners.LoggingListener;

/**
 * This class sets up and starts JUnit to excute the tests,
 * when the runTests function is called.
 * Before the tests are started, the preparation phase is executed.
 */
public class TestRunner {
    private static final Logger LOGGER = LogManager.getLogger();

    private final TestConfig testConfig;
    private final TestContext testContext;


    private boolean targetIsReady = false;

    public TestRunner(TestConfig testConfig, TestContext testContext) {
        this.testConfig = testConfig;
        this.testContext = testContext;
    }


    private void saveToCache(@Nonnull TestSiteReport report) {
        String fileName;
        if (testConfig.getTestEndpointMode() == TestEndpointType.CLIENT) {
            fileName = testConfig.getTestClientDelegate().getPort().toString();
        } else {
            fileName = testConfig.getTestServerDelegate().getHost();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker());
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            mapper.writeValue(new File(fileName + ".json"), report);

            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(report);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private TestSiteReport loadFromCache() {
        String fileName;
        if (testConfig.getTestEndpointMode() == TestEndpointType.CLIENT) {
            fileName = testConfig.getTestClientDelegate().getPort().toString();
        } else {
            fileName = testConfig.getTestServerDelegate().getHost();
        }

        File f = new File(fileName);
        if (f.exists() && !testConfig.isIgnoreCache()) {
            try {
                FileInputStream fis = new FileInputStream(fileName);
                ObjectInputStream ois = new ObjectInputStream(fis);
                LOGGER.info("Using cached siteReport");
                return (TestSiteReport) ois.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    private void waitForClient() {
        try {
            new Thread(() -> {
                while (!targetIsReady) {
                    LOGGER.warn("Waiting for the client to get ready...");
                    try {
                        State state = new State();
                        testConfig.getTestClientDelegate().executeTriggerScript(state);
                    } catch (Exception ignored) {}

                    try {
                        Thread.sleep(1000);
                    } catch (Exception ignored) {}
                }
            }).start();
            Socket socket = testConfig.getTestClientDelegate().getServerSocket().accept();
            targetIsReady = true;
            socket.close();
        } catch (Exception ignored) { }

        LOGGER.info("Client is ready, prepapring client exploration...");
    }

    private void waitForServer() {
        OutboundConnection connection = testConfig.createConfig().getDefaultClientConnection();

        try {
            Socket conTest = null;
            while (!targetIsReady) {
                try {
                    String connectionEndpoint;
                    if (connection.getHostname() != null) {
                        connectionEndpoint = connection.getHostname();
                    } else {
                        connectionEndpoint = connection.getIp();
                    }
                    conTest = new Socket(connectionEndpoint, connection.getPort());
                    targetIsReady = conTest.isConnected();
                } catch (Exception e) {
                    LOGGER.warn("Server not yet available (" + e.getLocalizedMessage() + ")");
                    Thread.sleep(1000);
                }
            }
            conTest.close();
        } catch (Exception e) {
            LOGGER.error(e);
            System.exit(2);
        }
    }


    private void serverTestPreparation() {
        waitForServer();

        TestSiteReport cachedReport = loadFromCache();
        if (cachedReport != null) {
            testContext.setSiteReport(cachedReport);
            return;
        }

        LOGGER.info("Server available, starting TLS-Scanner");
        ScannerConfig scannerConfig = new ScannerConfig(testConfig.getGeneralDelegate(), testConfig.getTestServerDelegate());
        scannerConfig.setTimeout(testConfig.getConnectionTimeout());
        Config config = scannerConfig.createConfig();
        config.setAddServerNameIndicationExtension(testConfig.createConfig().isAddServerNameIndicationExtension());

        config.getDefaultClientConnection().setConnectionTimeout(0);
        scannerConfig.setBaseConfig(config);

        scannerConfig.setProbes(
                ProbeType.COMMON_BUGS,
                ProbeType.CIPHER_SUITE,
                ProbeType.CERTIFICATE,
                ProbeType.COMPRESSIONS,
                ProbeType.NAMED_GROUPS,
                ProbeType.PROTOCOL_VERSION,
                ProbeType.EC_POINT_FORMAT,
                ProbeType.RESUMPTION,
                ProbeType.EXTENSIONS,
                ProbeType.RECORD_FRAGMENTATION,
                ProbeType.HELLO_RETRY
        );
        scannerConfig.setOverallThreads(1);
        scannerConfig.setParallelProbes(1);

        TlsScanner scanner = new TlsScanner(scannerConfig);

        TestSiteReport report = TestSiteReport.fromSiteReport(scanner.scan());
        saveToCache(report);

        testContext.setSiteReport(report);
        LOGGER.debug("TLS-Scanner finished!");
    }


    private void clientTestPreparation() {
        waitForClient();

        TestSiteReport cachedReport = loadFromCache();
        if (cachedReport != null) {
            testContext.setSiteReport(cachedReport);
            testContext.setReceivedClientHelloMessage(cachedReport.getReceivedClientHello());
            return;
        }

        List<TlsTask> tasks = new ArrayList<>();
        List<State> states = new ArrayList<>();

        List<CipherSuite> cipherList = CipherSuite.getImplemented();

        for (CipherSuite i: cipherList) {
            Config config = this.testConfig.createConfig();
            if (i.isTLS13()) {
                config = this.testConfig.createTls13Config();
            }

            config.setDefaultServerSupportedCipherSuites(Collections.singletonList(i));
            config.setDefaultSelectedCipherSuite(i);
            config.setEnforceSettings(true);

            try {
                WorkflowConfigurationFactory configurationFactory = new WorkflowConfigurationFactory(config);
                WorkflowTrace trace = configurationFactory.createWorkflowTrace(WorkflowTraceType.HANDSHAKE, RunningModeType.SERVER);
                State state = new State(config, trace);
                prepareStateForConnection(state);
                StateExecutionTask task = new StateExecutionTask(state, 2);
                task.setBeforeTransportInitCallback(testConfig.getTestClientDelegate().getTriggerScript());
                tasks.add(task);
                states.add(state);
            }
            catch(Exception ignored) {

            }
        }
        
        ParallelExecutor executor = new ParallelExecutor(testConfig.getParallelHandshakes(), 2);
        LOGGER.info("Executing client exploration with {} parallel threads...", testConfig.getParallelHandshakes());
        executor.bulkExecuteTasks(tasks);
        

        Set<CipherSuite> tls12CipherSuites = new HashSet<>();
        Set<CipherSuite> tls13CipherSuites = new HashSet<>();
        ClientHelloMessage clientHello = null;
        ReceiveAction clientHelloReceiveAction = null;
        long failed = 0;
        for (State s: states) {
            try {
                //allow additional app data sent by tls 1.3 client
                if(s.getConfig().getDefaultSelectedCipherSuite().isTLS13()) {
                    ReceiveAction lastReceive = (ReceiveAction)s.getWorkflowTrace().getReceivingActions().get(s.getWorkflowTrace().getReceivingActions().size() -1 );
                    ApplicationMessage appMsg = new ApplicationMessage();
                    appMsg.setRequired(false);
                    lastReceive.getExpectedMessages().add(appMsg);
                }
                if (s.getWorkflowTrace().executedAsPlanned()) {
                    if (clientHello == null) {
                        clientHello = s.getWorkflowTrace().getFirstReceivedMessage(ClientHelloMessage.class);
                        clientHelloReceiveAction = (ReceiveAction)s.getWorkflowTrace().getReceivingActions().get(0);
                    }
                    if (s.getTlsContext().getSelectedProtocolVersion() == ProtocolVersion.TLS12)
                        tls12CipherSuites.add(s.getConfig().getDefaultSelectedCipherSuite());
                    else if (s.getTlsContext().getSelectedProtocolVersion() == ProtocolVersion.TLS13)
                        tls13CipherSuites.add(s.getConfig().getDefaultSelectedCipherSuite());
                }
                else {
                    failed++;
                    LOGGER.debug("Workflow failed (" + s.getConfig().getDefaultSelectedCipherSuite() + ")");
                }
            } catch (Exception e) {
                LOGGER.error(e);
                throw new RuntimeException(e);
            }
        }
             
        List<State> keyShareStates = new LinkedList<>();
        List<TlsTask> keyShareTasks = new LinkedList<>();       
        if(clientHello.containsExtension(ExtensionType.ELLIPTIC_CURVES) && clientHello.containsExtension(ExtensionType.KEY_SHARE)) {
            keyShareStates = buildClientKeyShareProbeStates(clientHello);
            if(!keyShareStates.isEmpty()) {
                for(State state: keyShareStates) {
                    StateExecutionTask task = new StateExecutionTask(state, 2);
                    Connection connection = state.getConfig().getDefaultServerConnection();
                    try {
                        state.getTlsContext().setTransportHandler(new ServerTcpTransportHandler(testConfig.getConnectionTimeout(), testConfig.getConnectionTimeout(), testConfig.getTestClientDelegate().getServerSocket()));
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to set TransportHandler");
                    }
                    state.getTlsContext().setRecordLayer(
                    RecordLayerFactory.getRecordLayer(state.getTlsContext().getRecordLayerType(), state.getTlsContext()));
                    task.setBeforeTransportInitCallback(testConfig.getTestClientDelegate().getTriggerScript());
                    keyShareTasks.add(task);
                }
                executor.bulkExecuteTasks(keyShareTasks);
            }
        }
        List<NamedGroup> additionalTls13Groups = new LinkedList<>();
        for(State state: keyShareStates) {
            try {
                //test for Finished instead of asPlanned(), to ignore legacy CCS
                if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.FINISHED, state.getWorkflowTrace())) {
                    additionalTls13Groups.add(state.getConfig().getDefaultSelectedNamedGroup());
                }
                else {
                    failed++;
                    LOGGER.debug("Workflow failed (" + state.getConfig().getDefaultSelectedNamedGroup() + ")");
                }
            } catch (Exception e) {
                LOGGER.error(e);
                throw new RuntimeException(e);
            }
        }

        LOGGER.info(String.format("%d/%d client preparation workflows failed.", failed, states.size()));
        if (failed == states.size()) {
            throw new RuntimeException("Client preparation could not be completed.");
        }

        int rsaMinCertKeySize = getCertMinimumKeySize(executor, tls12CipherSuites, CertificateKeyType.RSA);
        int dssMinCertKeySize = getCertMinimumKeySize(executor, tls12CipherSuites, CertificateKeyType.DSS);
        boolean supportsRecordFragmentation = clientSupportsRecordFragmentation(executor, tls12CipherSuites, tls13CipherSuites);
        
        TestSiteReport report = new TestSiteReport("");
        report.addCipherSuites(tls12CipherSuites);
        report.addCipherSuites(tls13CipherSuites);
        report.setReceivedClientHello(clientHello);
        report.putResult(AnalyzedProperty.SUPPORTS_RECORD_FRAGMENTATION, supportsRecordFragmentation);
        report.setMinimumRsaCertKeySize(rsaMinCertKeySize);
        report.setMinimumDssCertKeySize(dssMinCertKeySize);
        additionalTls13Groups.addAll(report.getClientHelloKeyShareGroups());
        report.setSupportedTls13Groups(additionalTls13Groups);

        EllipticCurvesExtensionMessage ecExt = clientHello.getExtension(EllipticCurvesExtensionMessage.class);
        if (ecExt != null) {
            report.setSupportedNamedGroups(
                    NamedGroup.namedGroupsFromByteArray(ecExt.getSupportedGroups().getValue())
                        .stream()
                        .filter(i -> NamedGroup.getImplemented().contains(i))
                        .collect(Collectors.toList())
            );
        }

        SupportedVersionsExtensionMessage supportedVersionsExt = clientHello.getExtension(SupportedVersionsExtensionMessage.class);
        List<ProtocolVersion> versions = new ArrayList<>();
        versions.add(ProtocolVersion.getProtocolVersion(clientHello.getProtocolVersion().getValue()));
        versions.add(ProtocolVersion.getProtocolVersion(((Record)clientHelloReceiveAction.getReceivedRecords().get(0)).getProtocolVersion().getValue()));
        if (supportedVersionsExt != null) {
            versions.addAll(ProtocolVersion.getProtocolVersions(supportedVersionsExt.getSupportedVersions().getValue()));
        }
        report.setVersions(new ArrayList<>(new HashSet<>(versions)));

        SignatureAndHashAlgorithmsExtensionMessage sahExt = clientHello.getExtension(SignatureAndHashAlgorithmsExtensionMessage.class);
        if (sahExt != null) {
            report.setSupportedSignatureAndHashAlgorithms(
                    SignatureAndHashAlgorithm.getSignatureAndHashAlgorithms(sahExt.getSignatureAndHashAlgorithms().getValue()).stream()
                            .filter(i -> SignatureAndHashAlgorithm.getImplemented().contains(i))
                            .collect(Collectors.toList())
            );
        }

        List<ExtensionType> extensions = clientHello.getExtensions().stream()
                .map(i -> ExtensionType.getExtensionType(i.getExtensionType().getValue()))
                .collect(Collectors.toList());
        report.setSupportedExtensions(extensions);
        
        saveToCache(report);

        testContext.setReceivedClientHelloMessage(clientHello);
        testContext.setSiteReport(report);
        executor.shutdown();

    }

    public void prepareTestExecution() {
        if (!testConfig.isParsedArgs()) {
            return;
        }
        initParameterExtensions();

        ParallelExecutor executor = new ParallelExecutor(testConfig.getParallelHandshakes(), 2);
        executor.setTimeoutAction(testConfig.getTimeoutActionScript());
        executor.armTimeoutAction(20000);
        testContext.setStateExecutor(executor);

        // If some parameter extension (e.g. the ConfigurationOptionsExtension) already created a SiteReport we
        // need to skip this step here.
        if(testContext.getSiteReport() == null) {
            LOGGER.info("Starting preparation phase");
            this.testConfig.createConfig();

            if (this.testConfig.getTestEndpointMode() == TestEndpointType.CLIENT) {
                clientTestPreparation();
            } else if (this.testConfig.getTestEndpointMode() == TestEndpointType.SERVER) {
                serverTestPreparation();
            } else throw new RuntimeException("Invalid TestEndpointMode");

            if (testContext.getSiteReport() == null) {
                throw new RuntimeException("SiteReport is null after preparation phase");
            }
        }

        boolean targetSupportVersions = true;
        if (testConfig.getSupportedVersions() != null) {
            targetSupportVersions = false;
            for (ProtocolVersion i : testConfig.getSupportedVersions()) {
                if (testContext.getSiteReport().getVersions() != null && testContext.getSiteReport().getVersions().contains(i)) {
                    targetSupportVersions = true;
                    break;
                }
            }
        }

        boolean startTestSuite = false;
        if (testContext.getSiteReport().getVersions() == null || testContext.getSiteReport().getVersions().size() == 0) {
            LOGGER.error("Target does not support any ProtocolVersion");
        } else if (testContext.getSiteReport().getCipherSuites().size() == 0 && testContext.getSiteReport().getSupportedTls13CipherSuites().size() == 0) {
            LOGGER.error("Target does not support any CipherSuites");
        } else if (!targetSupportVersions) {
            LOGGER.error("Target does not support any ProtocolVersion that the Testsuite supports");
        } else {
            startTestSuite = true;
        }

        if (!startTestSuite) {
            System.exit(9);
        }

        LOGGER.info("Prepartion finished!");
    }


    private boolean countTests(TestIdentifier i, String versionS, String modeS) {
        TestSource source = i.getSource().orElse(null);
        if (!i.isTest() && (source == null || !source.getClass().equals(MethodSource.class))) {
            return false;
        }

        Set<TestTag> tags = i.getTags();
        boolean version = tags.stream().anyMatch(j -> j.getName().equals(versionS));
        boolean mode;
        if (!modeS.equals("both")) {
            mode = tags.stream().anyMatch(j -> j.getName().equals(modeS));
        } else {
            mode = tags.stream().noneMatch(j -> j.getName().equals("server"))
                    && tags.stream().noneMatch(j -> j.getName().equals("client"));
        }

        return version && mode;
    }

    public void runTests(Class<?> mainClass) {
        prepareTestExecution();

        String packageName = mainClass.getPackage().getName();
        if (testConfig.getTestPackage() != null) {
            packageName = testConfig.getTestPackage();
        }

        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                    selectPackage(packageName)
                )
                // https://junit.org/junit5/docs/current/user-guide/#writing-tests-parallel-execution
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "same_thread")
                .configurationParameter("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "fixed")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.parallelism",
                        String.valueOf(testConfig.getParallelTests())
                );
        
        if (testConfig.getTags().size() > 0) {
            builder.filters(
                    TagFilter.includeTags(testConfig.getTags())
            );
        }

        LauncherDiscoveryRequest request = builder.build();
        
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LoggingListener listenerLog = LoggingListener.forJavaUtilLogging(Level.INFO);
        ExecutionListener reporting = new ExecutionListener();

        Launcher launcher = LauncherFactory.create(
                LauncherConfig.builder()
                        .enableTestExecutionListenerAutoRegistration(false)
                        .addTestExecutionListeners(listener)
                        .addTestExecutionListeners(reporting)
                         //.addTestExecutionListeners(listenerLog)
                        .build()
        );
        
        TestPlan testplan = launcher.discover(request);
        long testcases = testplan.countTestIdentifiers(i -> {
            TestSource source = i.getSource().orElse(null);
            return i.isTest() || (source != null && source.getClass().equals(MethodSource.class));
        });
        long clientTls12 = testplan.countTestIdentifiers(i -> this.countTests(i, "tls12", "client"));
        long clientTls13 = testplan.countTestIdentifiers(i -> this.countTests(i, "tls13", "client"));
        long serverTls12 = testplan.countTestIdentifiers(i -> this.countTests(i, "tls12", "server"));
        long serverTls13 = testplan.countTestIdentifiers(i -> this.countTests(i, "tls13", "server"));
        long bothTls12 = testplan.countTestIdentifiers(i -> this.countTests(i, "tls12", "both"));
        long bothTls13 = testplan.countTestIdentifiers(i -> this.countTests(i, "tls13", "both"));
        
        LOGGER.info("Server tests, TLS 1.2: {}, TLS 1.3: {}", serverTls12 + bothTls12, serverTls13 + bothTls13);
        LOGGER.info("Client tests, TLS 1.2: {}, TLS 1.3: {}", clientTls12 + bothTls12, clientTls13 + bothTls13);
        LOGGER.info("Testing using default strength " + TestContext.getInstance().getConfig().getStrength());
        LOGGER.info("Default timeout " + TestContext.getInstance().getConfig().getConnectionTimeout() + " ms");
        logCommonDerivationValues();
        testContext.setTotalTests(testcases);
        long start = System.currentTimeMillis();

        launcher.execute(request);

        double elapsedTime = (System.currentTimeMillis() - start) / 1000.0;
        if (elapsedTime < 10) {
            LOGGER.error("Something seems to be wrong, testsuite executed in " + elapsedTime + "s");
        }

        TestExecutionSummary summary = listener.getSummary();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, true);
        summary.printTo(writer);
        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        LOGGER.info("\n" + content);

        testContext.getStateExecutor().shutdown();

        try {
            testConfig.getTestClientDelegate().getServerSocket().close();
        } catch (Exception e) {}

        System.exit(0);
    }
    
    private List<State> buildClientKeyShareProbeStates(ClientHelloMessage clientHello) {
        List<State> states = new ArrayList<>();
        EllipticCurvesExtensionMessage ecExtension = clientHello.getExtension(EllipticCurvesExtensionMessage.class);
        KeyShareExtensionMessage ksExtension = clientHello.getExtension(KeyShareExtensionMessage.class);
        List<NamedGroup> nonKeyShareCurves = NamedGroup.namedGroupsFromByteArray(ecExtension.getSupportedGroups().getValue());
        ksExtension.getKeyShareList().forEach(offeredKs -> nonKeyShareCurves.remove(offeredKs.getGroupConfig()));
        for (NamedGroup group: nonKeyShareCurves) {
            if (NamedGroup.getImplemented().contains(group)) {
                Config config = this.testConfig.createTls13Config();
                config.setDefaultServerNamedGroups(group);
                config.setDefaultSelectedNamedGroup(group);
                try {
                    WorkflowConfigurationFactory configurationFactory = new WorkflowConfigurationFactory(config);
                    WorkflowTrace trace = configurationFactory.createWorkflowTrace(WorkflowTraceType.HANDSHAKE, RunningModeType.SERVER);
                
                    ClientHelloMessage failingClientHello = new ClientHelloMessage();
                    ServerHelloMessage helloRetryRequest = new ServerHelloMessage(config);
                    helloRetryRequest.setRandom(Modifiable.explicit(ServerHelloMessage.getHelloRetryRequestRandom()));
        
                    trace.getTlsActions().add(0, new SendAction(helloRetryRequest));
                    trace.getTlsActions().add(0, new ReceiveAction(failingClientHello));
                
                    State s = new State(config, trace);
                    states.add(s);
                }
                catch(Exception ignored) {

                }
            }
        }
        return states;
    }
    
    
    
    private boolean clientSupportsRecordFragmentation(ParallelExecutor executor, Set<CipherSuite> tls12CipherSuites, Set<CipherSuite> tls13CipherSuites) {
        Config config = getServerConfigBasedOnCipherSuites(tls12CipherSuites, tls13CipherSuites);
        config.setDefaultMaxRecordData(50);

        State state = new State(config, new WorkflowConfigurationFactory(config).createWorkflowTrace(WorkflowTraceType.HANDSHAKE, RunningModeType.SERVER));
        prepareStateForConnection(state);
        StateExecutionTask task = new StateExecutionTask(state, 2);
        task.setBeforeTransportInitCallback(testConfig.getTestClientDelegate().getTriggerScript());
        executor.bulkExecuteTasks(task);
        return state.getWorkflowTrace().executedAsPlanned();
    }
    
    private Config getServerConfigBasedOnCipherSuites(Set<CipherSuite> tls12CipherSuites, Set<CipherSuite> tls13CipherSuites) {
        Config config;
        CipherSuite suite;
        if(!tls12CipherSuites.isEmpty()) {
            config = this.testConfig.createConfig();
            suite = tls12CipherSuites.iterator().next();     
        } else if(!tls13CipherSuites.isEmpty()) {
            config = this.testConfig.createTls13Config();
            suite = tls13CipherSuites.iterator().next(); 
        } else {
           throw new RuntimeException("No cipher suites detected"); 
        } 
        config.setDefaultServerSupportedCipherSuites(suite);
        config.setDefaultSelectedCipherSuite(suite);
        return config;
    }
    
    private int getCertMinimumKeySize(ParallelExecutor executor, Set<CipherSuite> cipherSuites, CertificateKeyType keyType) {
        List<CipherSuite> matchingCipherSuites = cipherSuites.stream().filter(cipherSuite -> AlgorithmResolver.getCertificateKeyType(cipherSuite) == keyType).collect(Collectors.toList());
        int minimumKeySize = 0;
        if(matchingCipherSuites.size() > 0) {
            List<State> certStates = getClientCertMinimumKeyLengthStates(matchingCipherSuites, keyType);
            List<TlsTask> certTasks = buildStateExecutionServerTasksFromStates(certStates);
            executor.bulkExecuteTasks(certTasks);
            for(State executedState: certStates) {
                int certKeySize = executedState.getConfig().getDefaultExplicitCertificateKeyPair().getPublicKey().keySize();
                if(executedState.getWorkflowTrace().executedAsPlanned() && (certKeySize < minimumKeySize || minimumKeySize == 0)) {
                    minimumKeySize = certKeySize;
                }
            }
        }
        return minimumKeySize;
    }
    
    private List<State> getClientCertMinimumKeyLengthStates(List<CipherSuite> supportedCipherSuites, CertificateKeyType keyType) {
        Set<CertificateKeyPair> availableCerts = new HashSet<>();
        CertificateByteChooser.getInstance().getCertificateKeyPairList().forEach(certKeyPair -> {
            if(certKeyPair.getCertPublicKeyType() == keyType) {
                availableCerts.add(certKeyPair);
            }
        });

        List<State> testStates = new LinkedList<>();
        for(CertificateKeyPair certKeyPair: availableCerts) {
            Config config = this.testConfig.createConfig();
            config.setAutoSelectCertificate(false);
            config.setDefaultExplicitCertificateKeyPair(certKeyPair);
            config.setDefaultServerSupportedCipherSuites(supportedCipherSuites);
            config.setDefaultSelectedCipherSuite(supportedCipherSuites.get(0));
            State state = new State(config, new WorkflowConfigurationFactory(config).createWorkflowTrace(WorkflowTraceType.HANDSHAKE, RunningModeType.SERVER));
            testStates.add(state);
        }
        return testStates;
    }
    
    private List<TlsTask> buildStateExecutionServerTasksFromStates(List<State> states) {
        List<TlsTask> testTasks = new LinkedList<>();
        states.forEach(state -> {
            prepareStateForConnection(state);
            StateExecutionTask task = new StateExecutionTask(state, 2);
            task.setBeforeTransportInitCallback(testConfig.getTestClientDelegate().getTriggerScript());
            testTasks.add(task);
        });
        return testTasks;
    }
    
    private void logCommonDerivationValues() {
        LOGGER.info("Supported NamedGroups:  " + TestContext.getInstance().getSiteReport().getSupportedNamedGroups().stream().map(NamedGroup::toString).collect(Collectors.joining(",")));
        LOGGER.info("Supported CipherSuites: " + TestContext.getInstance().getSiteReport().getCipherSuites().stream().map(CipherSuite::toString).collect(Collectors.joining(",")));
        if(TestContext.getInstance().getSiteReport().getSupportedTls13Groups() != null) {
            LOGGER.info("Supported TLS 1.3 NamedGroups: " + TestContext.getInstance().getSiteReport().getSupportedTls13Groups().stream().map(NamedGroup::toString).collect(Collectors.joining(",")));
        }
        if(TestContext.getInstance().getSiteReport().getSupportedTls13CipherSuites() != null) {
            LOGGER.info("Supported TLS 1.3 CipherSuites: " + TestContext.getInstance().getSiteReport().getSupportedTls13CipherSuites().stream().map(CipherSuite::toString).collect(Collectors.joining(",")));
        }
    }
    
    private void prepareStateForConnection(State state) {
        try {
            state.getTlsContext().setTransportHandler(new ServerTcpTransportHandler(testConfig.getConnectionTimeout(), testConfig.getConnectionTimeout(), testConfig.getTestClientDelegate().getServerSocket()));
            state.getTlsContext().setRecordLayer(
                    RecordLayerFactory.getRecordLayer(state.getTlsContext().getRecordLayerType(), state.getTlsContext()));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to set TransportHandlers");
        }
    }

    private void initParameterExtensions(){
        String configurationOptionsConfigFile = testConfig.getConfigOptionsConfigFile();
        if(!configurationOptionsConfigFile.isEmpty()){
            ParameterExtensionManager.getInstance().loadExtension("ConfigurationOptionsExtension", configurationOptionsConfigFile);
        }
    }
}
