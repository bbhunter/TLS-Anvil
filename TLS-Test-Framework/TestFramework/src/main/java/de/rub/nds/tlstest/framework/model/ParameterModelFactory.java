/**
 * TLS-Test-Framework - A framework for modeling TLS tests
 *
 * Copyright 2020 Ruhr University Bochum and
 * TÜV Informationstechnik GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlstest.framework.model;

import de.rub.nds.tlstest.framework.model.derivationParameter.BasicDerivationType;
import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlstest.framework.TestContext;
import de.rub.nds.tlstest.framework.constants.TestEndpointType;
import de.rub.nds.tlstest.framework.model.constraint.ConditionalConstraint;
import de.rub.nds.tlstest.framework.model.derivationParameter.DerivationParameter;
import de.rwth.swc.coffee4j.model.InputParameterModel;
import static de.rwth.swc.coffee4j.model.InputParameterModel.inputParameterModel;
import de.rwth.swc.coffee4j.model.Parameter;
import de.rwth.swc.coffee4j.model.constraints.Constraint;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides a model for Coffee4j or a SimpleTlsTest
 */
public class ParameterModelFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    public static InputParameterModel generateModel(DerivationScope derivationScope, TestContext testContext) {
        List<DerivationType> derivationTypes = getDerivationsForScope(derivationScope);
        Parameter.Builder[] builders = getModelParameters(derivationTypes, testContext, derivationScope);
        Constraint[] constraints = getModelConstraints(derivationTypes, derivationScope);

        return inputParameterModel("dynamic-model").strength(derivationScope.getTestStrength()).parameters(builders).exclusionConstraints(constraints).build();
    }

    public static List<DerivationType> getDerivationsForScope(DerivationScope derivationScope) {
        List<DerivationType> resultingDerivations = new LinkedList<>();
        List<DerivationType> derivationsOfModel = getDerivationsOfModel(derivationScope);
        for (DerivationType derivationType : BasicDerivationType.values()) {
            if (!isBeyondScope(derivationType, derivationsOfModel, derivationScope.getScopeLimits(), derivationScope.getScopeExtensions())) {
                resultingDerivations.add(derivationType);
            }
        }

        return resultingDerivations;
    }

    private static List<DerivationType> getDerivationsOfModel(DerivationScope derivationScope) {
        return getDerivationsOfModel(derivationScope, derivationScope.getBaseModel());
    }

    private static List<DerivationType> getDerivationsOfModel(DerivationScope derivationScope, ModelType baseModel) {
        /*LinkedList<DerivationType> derivationsOfModel = new LinkedList<>();
        switch (baseModel) {
            case EMPTY:
                break;
            case LENGTHFIELD:
            case CERTIFICATE:
                if (TestContext.getInstance().getConfig().getTestEndpointMode() == TestEndpointType.CLIENT) {
                    derivationsOfModel.add(BasicDerivationType.CERTIFICATE);
                    derivationsOfModel.add(BasicDerivationType.SIG_HASH_ALGORIHTM);
                }
            case GENERIC:
            default:
                derivationsOfModel.addAll(getBasicModelDerivations(derivationScope));
        }
        return derivationsOfModel;*/
        return DerivationManager.getInstance().getDerivationsOfModel(derivationScope, baseModel);
    }

    private static Parameter.Builder[] getModelParameters(List<DerivationType> derivationTypes, TestContext testContext, DerivationScope derivationScope) {
        List<Parameter.Builder> parameterBuilders = new LinkedList<>();
        for (DerivationType derivationType : derivationTypes) {
            DerivationParameter paramDerivation = DerivationManager.getInstance().getDerivationParameterInstance(derivationType);
            if (paramDerivation.canBeModeled(testContext, derivationScope)) {
                parameterBuilders.add(paramDerivation.getParameterBuilder(testContext, derivationScope));
                if (derivationType.isBitmaskDerivation()) {
                    DerivationParameter bitPositionParam = DerivationManager.getInstance().getDerivationParameterInstance(BasicDerivationType.BIT_POSITION);
                    bitPositionParam.setParent(derivationType);
                    parameterBuilders.add(bitPositionParam.getParameterBuilder(testContext, derivationScope));
                }
            }
        }

        return parameterBuilders.toArray(new Parameter.Builder[]{});
    }

    private static Constraint[] getModelConstraints(List<DerivationType> derivationTypes, DerivationScope scope) {
        List<Constraint> applicableConstraints = new LinkedList<>();
        for (DerivationType derivationType : derivationTypes) {
            if (DerivationManager.getInstance().getDerivationParameterInstance(derivationType).canBeModeled(TestContext.getInstance(), scope)) {
                List<ConditionalConstraint> condConstraints = DerivationManager.getInstance().getDerivationParameterInstance(derivationType).getConditionalConstraints(scope);
                for (ConditionalConstraint condConstraint : condConstraints) {
                    if (condConstraint.isApplicableTo(derivationTypes, scope)) {
                        applicableConstraints.add(condConstraint.getConstraint());
                    }
                }
            }
        }

        return applicableConstraints.toArray(new Constraint[]{});
    }

    private static boolean isBeyondScope(DerivationType derivationParameter, List<DerivationType> basicDerivations, List<DerivationType> scopeLimitations, List<DerivationType> scopeExtensions) {
        if ((!basicDerivations.contains(derivationParameter) && !scopeExtensions.contains(derivationParameter)) || scopeLimitations.contains(derivationParameter)) {
            return true;
        }
        return false;
    }

    /*private static List<DerivationType> getBasicModelDerivations(DerivationScope derivationScope) {
        List<DerivationType> derivationTypes = getBasicDerivationsForBoth(derivationScope);
        
        if(TestContext.getInstance().getConfig().getTestEndpointMode() == TestEndpointType.SERVER) {
            derivationTypes.addAll(getBasicDerivationsForServer(derivationScope));
        } else {
            derivationTypes.addAll(getBasicDerivationsForClient(derivationScope));
        }
        return derivationTypes;
    }*/
    
    /*private static List<DerivationType> getBasicDerivationsForBoth(DerivationScope derivationScope) {
        List<DerivationType> derivationTypes = new LinkedList<>();
        derivationTypes.add(BasicDerivationType.CIPHERSUITE);
        derivationTypes.add(BasicDerivationType.NAMED_GROUP);
        derivationTypes.add(BasicDerivationType.RECORD_LENGTH);
        derivationTypes.add(BasicDerivationType.TCP_FRAGMENTATION);

        if (derivationScope.isTls13Test()) {
            derivationTypes.add(BasicDerivationType.INCLUDE_CHANGE_CIPHER_SPEC);
        }
        
        return derivationTypes;
    }
    
    private static List<DerivationType> getBasicDerivationsForServer(DerivationScope derivationScope) {
        List<DerivationType> derivationTypes = new LinkedList<>();
        List<ExtensionType> supportedExtensions = TestContext.getInstance().getSiteReport().getSupportedExtensions();
        if (supportedExtensions != null) {
            //we add all extension regardless if the server negotiates them
            derivationTypes.add(BasicDerivationType.INCLUDE_ALPN_EXTENSION);
            derivationTypes.add(BasicDerivationType.INCLUDE_HEARTBEAT_EXTENSION);
            derivationTypes.add(BasicDerivationType.INCLUDE_PADDING_EXTENSION);
            derivationTypes.add(BasicDerivationType.INCLUDE_RENEGOTIATION_EXTENSION);
            derivationTypes.add(BasicDerivationType.INCLUDE_EXTENDED_MASTER_SECRET_EXTENSION);
            derivationTypes.add(BasicDerivationType.INCLUDE_SESSION_TICKET_EXTENSION);
            derivationTypes.add(BasicDerivationType.MAX_FRAGMENT_LENGTH);
            
            //we must know if the server negotiates Encrypt-Then-Mac to be able
            //to define correct constraints for padding tests
            if (supportedExtensions.contains(ExtensionType.ENCRYPT_THEN_MAC)) {
                derivationTypes.add(BasicDerivationType.INCLUDE_ENCRYPT_THEN_MAC_EXTENSION);
            }
            
            if (derivationScope.isTls13Test()) {
                derivationTypes.add(BasicDerivationType.INCLUDE_PSK_EXCHANGE_MODES_EXTENSION);
            }
        }
        
        if(TestContext.getInstance().getSiteReport().getResult(AnalyzedProperty.HAS_GREASE_CIPHER_SUITE_INTOLERANCE) != TestResult.TRUE) {
            derivationTypes.add(BasicDerivationType.INCLUDE_GREASE_CIPHER_SUITES);
        }
        
        if(TestContext.getInstance().getSiteReport().getResult(AnalyzedProperty.HAS_GREASE_NAMED_GROUP_INTOLERANCE) != TestResult.TRUE) {
            derivationTypes.add(BasicDerivationType.INCLUDE_GREASE_NAMED_GROUPS);
        }
        
        if(TestContext.getInstance().getSiteReport().getResult(AnalyzedProperty.HAS_GREASE_SIGNATURE_AND_HASH_ALGORITHM_INTOLERANCE) != TestResult.TRUE) {
            derivationTypes.add(BasicDerivationType.INCLUDE_GREASE_SIG_HASH_ALGORITHMS);
        }
        return derivationTypes;
    }
    
    private static List<DerivationType> getBasicDerivationsForClient(DerivationScope derivationScope) {
        List<DerivationType> derivationTypes = new LinkedList<>();
        if(!derivationScope.isTls13Test()) {
            if(TestContext.getInstance().getSiteReport().getReceivedClientHello().containsExtension(ExtensionType.ENCRYPT_THEN_MAC)) {
                derivationTypes.add(BasicDerivationType.INCLUDE_ENCRYPT_THEN_MAC_EXTENSION);
            }
            
            if(TestContext.getInstance().getSiteReport().getReceivedClientHello().containsExtension(ExtensionType.EXTENDED_MASTER_SECRET)) {
                derivationTypes.add(BasicDerivationType.INCLUDE_EXTENDED_MASTER_SECRET_EXTENSION);
            }
        }
        return derivationTypes;
    }*/

    /**
     * DerivationParameters that only have one possible value can not be modeled
     * by Coffee4J, we collect these here with their static value so the config
     * can be set up properly
     */
    public static List<DerivationParameter> getStaticParameters(TestContext context, DerivationScope scope) {
        List<DerivationParameter> staticParameters = new LinkedList<>();
        List<DerivationType> plannedDerivations = getDerivationsForScope(scope);
        for (DerivationType type : plannedDerivations) {
            List<DerivationParameter> parameterValues = DerivationManager.getInstance().getDerivationParameterInstance(type).getConstrainedParameterValues(context, scope);
            if (parameterValues.size() == 1) {
                staticParameters.add(parameterValues.get(0));
            }
        }
        return staticParameters;
    }

    public static boolean mustUseSimpleModel(TestContext context, DerivationScope scope) {
        List<DerivationType> derivationTypes = getDerivationsForScope(scope);
        Parameter.Builder[] builders = getModelParameters(derivationTypes, context, scope);
        return builders.length == 1;
    }

    public static List<DerivationParameter> getSimpleModelVariations(TestContext context, DerivationScope scope) {
        List<DerivationType> modelDerivations = getDerivationsForScope(scope);
        for (DerivationType type : modelDerivations) {
            DerivationParameter parameter = DerivationManager.getInstance().getDerivationParameterInstance(type);
            if (parameter.canBeModeled(context, scope)) {
                return parameter.getConstrainedParameterValues(context, scope);
            }
        }
        return null;
    }
}
