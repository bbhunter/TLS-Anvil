/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * <p>Copyright 2022 Ruhr University Bochum
 *
 * <p>Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.extractor;

import de.rub.nds.anvilcore.annotation.AnvilTest;
import de.rub.nds.anvilcore.annotation.NonCombinatorialAnvilTest;
import de.rub.nds.anvilcore.constants.TestEndpointType;
import de.rub.nds.anvilcore.junit.extension.EndpointConditionExtension;
import de.rub.nds.anvilcore.teststate.reporting.MetadataFetcher;
import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.annotations.EnforcedSenderRestriction;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

public class TestCaseExtractor {
    private static final Logger LOGGER = LogManager.getLogger(TestCaseExtractor.class);

    private String packageName;
    private static MetadataFetcher fetcher;

    public TestCaseExtractor(String packageName) {
        this.packageName = packageName;
    }

    public void start() {
        boolean detailedOutput =
                TestContext.getInstance().getConfig().getTestExtractorDelegate().isDetailed();

        fetcher = new MetadataFetcher();

        Reflections reflections = new Reflections(packageName, new MethodAnnotationsScanner());
        Set<Method> testMethodsRaw = reflections.getMethodsAnnotatedWith(AnvilTest.class);
        testMethodsRaw.addAll(reflections.getMethodsAnnotatedWith(NonCombinatorialAnvilTest.class));

        Set<ExtractionMethod> testMethods =
                testMethodsRaw.stream()
                        .filter(i -> i.getAnnotation(EnforcedSenderRestriction.class) == null)
                        .map(ExtractionMethod::new)
                        .filter(i -> i.getRfcNumber() != null)
                        .filter(i -> !i.getDescription().matches("^[\\s\\n]*$"))
                        .collect(Collectors.toSet());

        Set<ExtractionMethod> stateMachineMethods =
                testMethodsRaw.stream()
                        .filter(i -> i.getDeclaringClass().getName().contains("statemachine"))
                        .map(ExtractionMethod::new)
                        // some lengthfield and state machine tests do have an RFC identifier
                        .filter(
                                i ->
                                        !testMethods.stream()
                                                .map(ExtractionMethod::getFullTestName)
                                                .anyMatch(
                                                        fullTestName ->
                                                                fullTestName.equals(
                                                                        i.getFullTestName())))
                        .collect(Collectors.toSet());

        Set<ExtractionMethod> lengthfieldMethods =
                testMethodsRaw.stream()
                        .filter(i -> (i.getDeclaringClass().getName().contains("lengthfield")))
                        .map(ExtractionMethod::new)
                        // some lengthfield and state machine tests do have an RFC identifier
                        .filter(
                                i ->
                                        !testMethods.stream()
                                                .map(ExtractionMethod::getFullTestName)
                                                .anyMatch(
                                                        fullTestName ->
                                                                fullTestName.equals(
                                                                        i.getFullTestName())))
                        .collect(Collectors.toSet());

        Set<ExtractionMethod> nonMandatoryTests =
                testMethods.stream()
                        .filter(
                                extractionMethod ->
                                        !(extractionMethod.getDescription().contains("MUST")
                                                || extractionMethod
                                                        .getDescription()
                                                        .contains("SHALL")
                                                || extractionMethod
                                                        .getDescription()
                                                        .contains("REQUIRED")))
                        .collect(Collectors.toSet());

        Set<ExtractionMethod> consideredTestsWithCt = new HashSet<>(testMethods);
        consideredTestsWithCt.addAll(stateMachineMethods);
        consideredTestsWithCt.addAll(lengthfieldMethods);
        consideredTestsWithCt =
                consideredTestsWithCt.stream()
                        .filter(
                                extractionMethod ->
                                        extractionMethod.m.getAnnotation(AnvilTest.class) != null)
                        .collect(Collectors.toSet());

        LOGGER.info(
                "Found {} RFC tests, {} of which without a mandatory statement, and additionally {} State Machine and {} Lengthfield Tests (Total unique test templates considered: {})",
                testMethods.size(),
                nonMandatoryTests.size(),
                stateMachineMethods.size(),
                lengthfieldMethods.size(),
                testMethods.size() + stateMachineMethods.size() + lengthfieldMethods.size());
        LOGGER.info("Among these, {} use CT", consideredTestsWithCt.size());
        if (detailedOutput) {
            LOGGER.info(
                    "Non-Mandatory tests: \n{}",
                    String.join(
                            "\n",
                            nonMandatoryTests.stream()
                                    .map(ExtractionMethod::getFullTestName)
                                    .collect(Collectors.joining("\n"))));
        }

        Map<Integer, List<ExtractionMethod>> rfcMap = new HashMap<>();
        testMethods.forEach(
                i -> {
                    int rfcNumber = i.getRfcNumber();
                    if (!rfcMap.containsKey(rfcNumber)) {
                        rfcMap.put(rfcNumber, new ArrayList<>());
                    }

                    rfcMap.get(rfcNumber).add(i);
                });

        Set<ExtractionMethod> testsWithQuotesFound = new HashSet<>();
        rfcMap.keySet()
                .forEach(
                        rfcNumber -> {
                            List<ExtractionMethod> testCases = rfcMap.get(rfcNumber);
                            RFCHtml rfcHtml = new RFCHtml(rfcNumber);
                            applyHtmlRFCAnnotations(rfcHtml, rfcNumber);
                            LOGGER.info("RFC {}: Found {} test cases", rfcNumber, testCases.size());

                            for (ExtractionMethod testCase : testCases) {
                                boolean foundQuote =
                                        rfcHtml.markText(
                                                testCase.getDescription(),
                                                HtmlRFCAnnotation.COVERED,
                                                true);
                                if (foundQuote) {
                                    testsWithQuotesFound.add(testCase);
                                }
                            }

                            if (detailedOutput) {
                                LOGGER.info(
                                        "MUST (NOT) coverage RFC {}: \n{}",
                                        rfcNumber,
                                        rfcHtml.getPrintableCounters());
                            }
                            rfcHtml.saveToFolder(
                                    TestContext.getInstance()
                                            .getConfig()
                                            .getTestExtractorDelegate()
                                            .getOutputFolder());
                        });

        if (detailedOutput) {
            printTestsWithoutQuote(testMethods, testsWithQuotesFound);
            printClientServerTestCounters(rfcMap, lengthfieldMethods, stateMachineMethods);
            printTestRfcTestMap(rfcMap);
        }
    }

    private void printTestsWithoutQuote(
            Set<ExtractionMethod> testMethods, Set<ExtractionMethod> testsWithQuotesFound) {
        Set<ExtractionMethod> testsWithoutQuote = new HashSet<>(testMethods);
        testsWithoutQuote.removeAll(testsWithQuotesFound);
        LOGGER.info(
                "Non-Quoted Tests ({}): \n{}",
                testsWithoutQuote.size(),
                String.join(
                        "\n",
                        testsWithoutQuote.stream()
                                .map(ExtractionMethod::getFullTestName)
                                .collect(Collectors.joining("\n"))));
    }

    private void printClientServerTestCounters(
            Map<Integer, List<ExtractionMethod>> rfcMap,
            Set<ExtractionMethod> lengthfieldMethods,
            Set<ExtractionMethod> stateMachineMethods) {
        LOGGER.info("Number of tests for client and server (LaTeX)");
        Map<Integer, Integer> rfcCountMap = new HashMap<>();
        rfcMap.keySet()
                .forEach(rfcNumber -> rfcCountMap.put(rfcNumber, rfcMap.get(rfcNumber).size()));
        List<Entry<Integer, Integer>> entryList = new LinkedList<>(rfcCountMap.entrySet());
        entryList.sort(Entry.comparingByValue());
        Collections.reverse(entryList);

        for (Entry<Integer, Integer> rfcNumberEntry : entryList) {
            int clientOrBoth =
                    countTestsForEndpoint(
                            rfcMap.get(rfcNumberEntry.getKey()), TestEndpointType.CLIENT);
            int serverOrBoth =
                    countTestsForEndpoint(
                            rfcMap.get(rfcNumberEntry.getKey()), TestEndpointType.SERVER);
            LOGGER.info(
                    "RFC {} & {} & {} \\\\", rfcNumberEntry.getKey(), clientOrBoth, serverOrBoth);
        }
        int lengthfieldClientBoth =
                countTestsForEndpoint(lengthfieldMethods, TestEndpointType.CLIENT);
        int lengthfieldServerBoth =
                countTestsForEndpoint(lengthfieldMethods, TestEndpointType.SERVER);
        int statemachineClientBoth =
                countTestsForEndpoint(stateMachineMethods, TestEndpointType.CLIENT);
        int statemachineServerBoth =
                countTestsForEndpoint(stateMachineMethods, TestEndpointType.SERVER);
        LOGGER.info("Lengthfield & {} & {}  \\\\", lengthfieldClientBoth, lengthfieldServerBoth);
        LOGGER.info("Statemachine & {} & {}  \\\\", statemachineClientBoth, statemachineServerBoth);
    }

    private int countTestsForEndpoint(
            Collection<ExtractionMethod> collection, TestEndpointType endpointType) {
        return (int)
                collection.stream()
                        .filter(
                                testCase ->
                                        testCase.getEndpointType() == endpointType
                                                || testCase.getEndpointType()
                                                        == TestEndpointType.BOTH)
                        .count();
    }

    private void printTestRfcTestMap(Map<Integer, List<ExtractionMethod>> rfcMap) {
        LOGGER.info("RFC-Test-Map:");
        for (int rfc : rfcMap.keySet()) {
            LOGGER.info(
                    "RFC {}: \n{}",
                    rfc,
                    rfcMap.get(rfc).stream()
                            .map(ExtractionMethod::getFullTestName)
                            .collect(Collectors.joining("\n")));
        }
    }

    private void applyHtmlRFCAnnotations(RFCHtml rfcHtml, int rfcNumber) {
        rfcHtml.findMustAndNotPositions();
        Map<HtmlRFCAnnotation, List<String>> annotationMap =
                HtmlRFCAnnotation.getAnnotations(rfcNumber, "annotations/");
        for (HtmlRFCAnnotation annotationType : annotationMap.keySet()) {
            for (String annotatedPassage : annotationMap.get(annotationType)) {
                rfcHtml.markText(annotatedPassage, annotationType, true);
            }
        }
    }

    private static class ExtractionMethod {
        private String testId;
        private final Method m;
        private Integer rfc;
        private String section;
        private String description;

        public ExtractionMethod(Method method) {
            this.m = method;
            NonCombinatorialAnvilTest NonCombinatorialAnvilTest =
                    m.getAnnotation(NonCombinatorialAnvilTest.class);
            AnvilTest AnvilTest = m.getAnnotation(AnvilTest.class);
            this.testId = method.getDeclaringClass().getName() + "." + method.getName();
            if (NonCombinatorialAnvilTest != null && !NonCombinatorialAnvilTest.id().isEmpty()) {
                this.testId = NonCombinatorialAnvilTest.id();
            } else if (AnvilTest != null && !AnvilTest.id().isEmpty()) {
                this.testId = AnvilTest.id();
            }
            Map<?, ?> meta = fetcher.getRawMetadata(testId);
            if (meta.get("rfc") != null) {
                this.rfc = (int) ((Map<?, ?>) meta.get("rfc")).get("number");
                this.section = (String) ((Map<?, ?>) meta.get("rfc")).get("section");
            }
            description = (String) meta.get("description");
        }

        public String getDescription() {
            if (description.matches("^[\\s\\n]*$")) {
                LOGGER.warn("Empty description {}", m);
            }
            return description;
        }

        public TestEndpointType getEndpointType() {
            return EndpointConditionExtension.endpointOfMethod(m, m.getDeclaringClass());
        }

        public Integer getRfcNumber() {
            return rfc;
        }

        public String getFullTestName() {
            return m.getDeclaringClass().getName() + "." + m.getName();
        }

        public String getTestName() {
            return m.getName();
        }
    }
}
