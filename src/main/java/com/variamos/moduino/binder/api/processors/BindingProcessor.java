package com.variamos.moduino.binder.api.processors;

import com.variamos.moduino.binder.api.json.binding.*;
import com.variamos.moduino.binder.api.json.binding.actions.*;
import com.variamos.moduino.binder.api.json.binding.relations.*;
import com.variamos.moduino.binder.api.json.binding.relations.base.CommonExecutionActionJson;
import com.variamos.moduino.binder.api.json.binding.relations.base.CommonPhaseJson;
import com.variamos.moduino.binder.api.json.binding.relations.base.CommonPredicateJson;
import com.variamos.moduino.binder.api.json.binding.relations.hardwareBehavior.ActionResultJson;
import com.variamos.moduino.binder.api.json.binding.relations.hardwareBehavior.ActionVariableJson;
import com.variamos.moduino.binder.api.json.component.BindingComponentJson;
import com.variamos.moduino.binder.api.json.hardware.DeviceJson;
import com.variamos.moduino.binder.api.structure.BindingComponent;
import com.variamos.moduino.binder.api.structure.StructureModifier;
import com.variamos.moduino.binder.api.structure.dynamic.*;
import com.variamos.moduino.binder.api.type.PredicateComponentType;
import com.variamos.moduino.binder.api.type.VariableComponentType;
import com.variamos.moduino.binder.model.library.SketchHeaderPidLibrary;
import com.variamos.moduino.binder.model.variables.SketchLiquidCrystalVariable;
import com.variamos.moduino.binder.model.variables.SketchPidVariable;
import com.variamos.moduino.binder.resolver.processors.DeviceProcessor;
import com.variamos.moduino.binder.resolver.processors.data.DeviceWriter;
import me.itoxic.moduino.generator.buffer.CodeBuffer;
import me.itoxic.moduino.metamodel.arduino.entries.model.pins.AnalogPin;
import me.itoxic.moduino.metamodel.arduino.entries.model.pins.DigitalPin;
import me.itoxic.moduino.metamodel.arduino.entries.model.uno.ArduinoUnoBoard;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.SketchFunction;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.SketchInstruction;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.SketchVariable;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.conditions.SketchBooleanCondition;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.conditions.SketchIntegerCondition;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.conditions.comparator.SketchIntegerComparatorType;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.function.SketchFunctionCall;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.function.type.SketchFunctionType;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.operations.SketchElseOperation;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.operations.SketchIfOperation;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.preloads.SketchNativeFunctionType;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.preprocessor.SketchDefineDirective;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.preprocessor.SketchIncludeDirective;
import me.itoxic.moduino.metamodel.arduino.entries.sketch.variables.*;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

@Getter
public class BindingProcessor extends Processor<BindingComponentJson> {

    private DeviceProcessor deviceProcessor;
    private MachineProcessor machineProcessor;
    private HardwareProcessor hardwareProcessor;

    private LinkedList<ActivityJson> activityJsons;
    private LinkedList<LogicalOperatorJson> logicalOperatorJsons;
    private LinkedList<PredicateJson> predicateJsons;
    private LinkedList<ReadPortJson> readPortJsons;
    private LinkedList<OperatorPredicateJson> operatorPredicateJsons;
    private LinkedList<OperatorTransitionJson> operatorTransitionJsons;
    private LinkedList<PredicateVariablesJson> predicateVariablesJsons;
    private LinkedList<ActionVariableJson> actionVariableJsons;
    private LinkedList<ControlActionPortJson> controlActionPortJsons;
    private LinkedList<VariableJson> variableJsons;
    private LinkedList<ActionResultJson> actionResultJsons;
    private LinkedList<DeviceActionJson> deviceActionJsons;

    private LinkedList<DeviceWriteActionData> deviceWriteActionDatas;
    private LinkedList<WriteActionData> writeActionDatas;
    private LinkedList<ReadActionData> readActionDatas;
    private LinkedList<ControlActionData> controlActionDatas;
    private LinkedList<TimerData> timerDatas;

    private SketchFunction resetTimer;

    public BindingProcessor(StructureModifier structureModifier, DeviceProcessor deviceProcessor, HardwareProcessor hardwareProcessor, MachineProcessor machineProcessor) {
        super(structureModifier);

        this.machineProcessor = machineProcessor;
        this.hardwareProcessor = hardwareProcessor;
        this.deviceProcessor = deviceProcessor;

        this.activityJsons = new LinkedList<>();
        this.predicateJsons = new LinkedList<>();
        this.readPortJsons = new LinkedList<>();
        this.logicalOperatorJsons = new LinkedList<>();
        this.operatorPredicateJsons = new LinkedList<>();
        this.operatorTransitionJsons = new LinkedList<>();
        this.predicateVariablesJsons = new LinkedList<>();
        this.controlActionPortJsons = new LinkedList<>();
        this.variableJsons = new LinkedList<>();
        this.actionVariableJsons = new LinkedList<>();
        this.actionResultJsons = new LinkedList<>();
        this.deviceActionJsons = new LinkedList<>();

        this.timerDatas = new LinkedList<>();
        this.writeActionDatas = new LinkedList<>();
        this.readActionDatas = new LinkedList<>();
        this.controlActionDatas = new LinkedList<>();
        this.deviceWriteActionDatas = new LinkedList<>();

    }

    @Override
    public void compose(List<BindingComponentJson> componentJson, ArduinoUnoBoard board) {

        LinkedList<BindingComponent<?>> bindingComponents = getBindingComponents(componentJson);

        iterator:
        for (BindingComponent<?> bindingComponent : bindingComponents) {

            switch (bindingComponent.getType()) {

                case VARIABLE:

                    VariableJson variableJson = bindingComponent.getGenericComponent();
                    variableJsons.add(variableJson);

                    if (variableJson.getType() == VariableComponentType.ANALOG_VARIABLE) {

                        Double value = Double.parseDouble(variableJson.getValue());
                        SketchDoubleVariable variable = new SketchDoubleVariable(variableJson.getLabel(), value);

                        board.getSketch().addVariable(variable);
                        getStructure().getOrPut(variableJson.getId(), variable);
                        continue iterator;

                    } else if (variableJson.getType() == VariableComponentType.DIGITAL_VARIABLE) {

                        Integer value = variableJson.getValue().equals("LOW") ? 0 : 1;
                        SketchIntegerVariable variable = new SketchIntegerVariable(variableJson.getLabel(), value);

                        board.getSketch().addVariable(variable);
                        getStructure().getOrPut(variableJson.getId(), variable);
                        continue iterator;

                    } else if (variableJson.getType() == VariableComponentType.STRING_VARIABLE) {

                        String value = variableJson.getValue();
                        SketchStringVariable variable = new SketchStringVariable(variableJson.getLabel(), value);

                        board.getSketch().addVariable(variable);
                        getStructure().getOrPut(variableJson.getId(), variable);
                        continue iterator;

                    }

                    break;

                case CONTROL_ACTION:

                    ControlActionJson controlActionJson = bindingComponent.getGenericComponent();
                    ControlActionData controlActionData = new ControlActionData(controlActionJson);

                    controlActionData.setId(controlActionJson.getId());
                    controlActionData.setLabel(controlActionJson.getLabel());

                    if(controlActionJson.isContinuous()) {

                        SketchFunction wfunction = new SketchFunction("action_" + controlActionJson.getLabel(), SketchFunctionType.VOID, false);

                        controlActionData.setFunction(wfunction);
                        board.getSketch().addFunction(wfunction);
                        getStructure().getOrPut(controlActionJson.getId(), wfunction);

                    }

                    controlActionDatas.add(controlActionData);
                    break;

                case TIMER:

                    TimerJson timerJson = bindingComponent.getGenericComponent();
                    TimerData timerData = new TimerData(timerJson);

                    Long value = Long.parseLong(timerJson.getInitialValue());

                    SketchLongVariable variable = new SketchLongVariable(timerJson.getLabel(), value);
                    SketchFunctionCall call = new SketchFunctionCall(SketchNativeFunctionType.MILLIS);

                    variable.setUnsigned(true);
                    variable.addComputedFunction(call);

                    timerData.setVariable(variable);
                    board.getSketch().addVariable(variable);
                    getStructure().getOrPut(timerJson.getId(), variable);

                    timerDatas.add(timerData);
                    break;

                case ACTION_RESULT:
                    ActionResultJson actionResultJson = bindingComponent.getGenericComponent();
                    actionResultJsons.add(actionResultJson);
                    break;

                case READ_ACTION:

                    ReadActionJson readActionJson = bindingComponent.getGenericComponent();
                    ReadActionData readActionData = new ReadActionData(readActionJson);

                    //SketchFunction rfunction = new SketchFunction("action_" + readActionJson.getLabel(), readActionJson.getLabel().equals("leer_teclado") || readActionJson.getLabel().equals("leer_tecla") ? SketchFunctionType.STRING : SketchFunctionType.FLOAT, false);
                    SketchFunction rfunction = new SketchFunction("action_" + readActionJson.getLabel(),  SketchFunctionType.VOID, false);

                    board.getSketch().addFunction(rfunction);
                    readActionData.setFunction(rfunction);
                    getStructure().getOrPut(readActionJson.getId(), rfunction);
                    readActionDatas.add(readActionData);
                    break;

                case ACTION_VARIABLE:
                    ActionVariableJson actionVariableJson = bindingComponent.getGenericComponent();
                    actionVariableJsons.add(actionVariableJson);
                    break;

                case WRITE_ACTION: // todo

                    WriteActionJson writeActionJson = bindingComponent.getGenericComponent();
                    WriteActionData writeActionData = new WriteActionData(writeActionJson);

                    SketchFunction wfunction = new SketchFunction("action_" + writeActionJson.getLabel(), SketchFunctionType.VOID, false);

                    writeActionData.setFunction(wfunction);
                    board.getSketch().addFunction(wfunction);
                    getStructure().getOrPut(writeActionJson.getId(), wfunction);

                    writeActionDatas.add(writeActionData);
                    break;

                case PREDICATE:

                    PredicateJson predicateJson = bindingComponent.getGenericComponent();
                    predicateJsons.add(predicateJson);
                    break;

                case ACTIVITY:

                    ActivityJson activityJson = bindingComponent.getGenericComponent();
                    SketchFunction sketchFunction = new SketchFunction(activityJson.getLabel(), SketchFunctionType.VOID, false);

                    board.getSketch().addFunction(sketchFunction);
                    getStructure().getOrPut(activityJson.getId(), sketchFunction);

                    activityJsons.add(activityJson);
                    break;

                case ACTIVITY_ACTION: //coco
                    ActivityActionJson activityActionJson=bindingComponent.getGenericComponent();
                    ActivityJson activity = getActivity(activityActionJson.getActivity());
                    SketchFunction function = getStructure().getFunction(activity.getId());
                    for (CommonExecutionActionJson commonExecutionActionJson : activityActionJson.getActions()) {
                        if (commonExecutionActionJson.getActionType().equals("writeAction")){
                            WriteActionData data = getWriteAction(commonExecutionActionJson.getAction());
                            SketchFunctionCall functionCall = new SketchFunctionCall(data.getSketchFunction());
                            function.addInstruction(functionCall);
                        } else if (commonExecutionActionJson.getActionType().equals("readAction")){
                            ReadActionData data=getReadAction(commonExecutionActionJson.getAction());
                            SketchFunctionCall functionCall = new SketchFunctionCall(data.getSketchFunction());
                            function.addInstruction(functionCall);
                        } else if (commonExecutionActionJson.getActionType().equals("controlAction")){
                            ControlActionData data = getControlAction(commonExecutionActionJson.getAction());
                            SketchFunctionCall functionCall = new SketchFunctionCall(data.getSketchFunction());
                            function.addInstruction(functionCall);
                        }
                    }
                    break;


                /*case DEVICE_PORT:

                    int i=0;

                    ActivityWriteJson activityWriteJson = bindingComponent.getGenericComponent();
                    ActivityJson activity = getActivity(activityWriteJson.getActivity());

                    SketchFunction function = getStructure().getFunction(activity.getId());

                    for (CommonWriteActionJson commonWriteActionJson : activityWriteJson.getWriteActions()) {

                        WriteActionData data = getWriteAction(commonWriteActionJson.getWriteAction());
                        SketchFunctionCall functionCall = new SketchFunctionCall(data.getSketchFunction());

                        function.addInstruction(functionCall);
                        continue;

                    }

                    break;*/

                case CONTROL_ACTION_PORT:

                    ControlActionPortJson controlActionPortJson = bindingComponent.getGenericComponent();
                    controlActionPortJsons.add(controlActionPortJson);
                    break;

                case PREDICATE_VARIABLES:
                    PredicateVariablesJson predicateVariablesJson = bindingComponent.getGenericComponent();
                    predicateVariablesJsons.add(predicateVariablesJson);
                    break;

                case LOGICAL_OPERATOR:

                    LogicalOperatorJson logicalOperatorJson = bindingComponent.getGenericComponent();
                    logicalOperatorJsons.add(logicalOperatorJson);
                    break;

                case OPERATOR_TRANSITION:

                    OperatorTransitionJson operatorTransitionJson = bindingComponent.getGenericComponent();
                    operatorTransitionJsons.add(operatorTransitionJson);
                    break;

                case OPERATOR_PREDICATE:

                    OperatorPredicateJson operatorPredicateJson = bindingComponent.getGenericComponent();
                    operatorPredicateJsons.add(operatorPredicateJson);
                    break;

                case READ_PORT:

                    ReadPortJson readPortJson = bindingComponent.getGenericComponent();
                    readPortJsons.add(readPortJson);
                    break;

                case DEVICE_ACTION:
                    DeviceActionJson deviceActionJson = bindingComponent.getGenericComponent();
                    deviceActionJsons.add(deviceActionJson);
                    break;

                case WRITE_PORT:

                    WritePortJson writePortJson = bindingComponent.getGenericComponent();
                    WriteActionData actionData = getWriteAction(writePortJson.getWriteAction());

                    SketchVariable writeVariable = getStructure().getVariable(writePortJson.getReadPort());

                    if (writeVariable instanceof SketchFloatVariable) {

                        SketchFloatVariable floatVariable = (SketchFloatVariable) writeVariable;
                        DigitalPin digitalPin = getStructure().getDigitalPin(writePortJson.getWritePort());

                        if (digitalPin != null) {
                            actionData.getSketchFunction().addInstruction(digitalPin.getWriteInstruction(floatVariable));
                            break;
                        }

                        AnalogPin analogPin = getStructure().getAnalogPin(writePortJson.getWritePort());

                        if (analogPin != null) {
                            break;
                        }

                        TimerData tData = getTimer(writePortJson.getWritePort());

                        if (tData != null) {

                            SketchLongVariable sketchLongVariable = tData.getSketchLongVariable();
                            actionData.getSketchFunction().addInstruction(sketchLongVariable.redefineVariablesAsMillis());

                        }

                        break;

                    }

                    if (writeVariable instanceof SketchStringVariable) {

                        SketchStringVariable stringVariable = (SketchStringVariable) writeVariable;
                        SketchLibraryVariable sketchLibraryVariable = getStructure().getLibraryVariable(writePortJson.getWritePort());

                        if (sketchLibraryVariable != null) {

                            if (sketchLibraryVariable instanceof SketchLiquidCrystalVariable) {

                                SketchLiquidCrystalVariable sketchLiquidCrystalVariable = (SketchLiquidCrystalVariable) sketchLibraryVariable;
                                actionData.getSketchFunction().addInstruction(sketchLiquidCrystalVariable.operatePrintText(stringVariable));

                            }

                            break;

                        }
                        break;

                    }

                    DigitalPin digitalPin = getStructure().getDigitalPin(writePortJson.getWritePort());

                    if (digitalPin != null) {

                        SketchIntegerVariable digitalNumberVariable = getStructure().getVariable(writePortJson.getReadPort());
                        actionData.getSketchFunction().addInstruction(digitalPin.getWriteInstruction(digitalNumberVariable));
                        break;

                    }

                    break;

                case STATE_ACTIVITY:

                    SketchIntegerVariable currentState = getStructure().getVariable("flag_state");
                    SketchIntegerVariable lastState = getStructure().getVariable("flag_last_state");

                    if (currentState == null || lastState == null)
                        break;

                    StateActivityJson stateActivityJson = bindingComponent.getGenericComponent();
                    BasicStateData basicStateData = machineProcessor.getState(stateActivityJson.getState());

                    SketchIntegerCondition sketchIntegerCondition = new SketchIntegerCondition(lastState, currentState, SketchIntegerComparatorType.NOT_EQUALS);
                    SketchIfOperation sketchIfOperation = new SketchIfOperation(sketchIntegerCondition);

                    if (stateActivityJson.getBeginPhase().size() > 0)
                        for (CommonPhaseJson beginPhase : stateActivityJson.getBeginPhase())
                            sketchIfOperation.addInstruction(new SketchFunctionCall(getStructure().getFunction(beginPhase.getActivity())));

                    if (stateActivityJson.getWhilePhase().size() > 0) {
                        SketchElseOperation sketchElseOperation = new SketchElseOperation(sketchIfOperation);
                        for (CommonPhaseJson whilePhase : stateActivityJson.getWhilePhase()) {

                            sketchElseOperation.addInstruction(new SketchFunctionCall(getStructure().getFunction(whilePhase.getActivity())));

                        }
                    }

                    sketchIfOperation.addInstruction(lastState.redefineVariable(currentState));
                    basicStateData.getSketchFunction().addInstruction(sketchIfOperation);

                    LinkedList<BasicTransitionData> stateTransitions = machineProcessor.getProperlyTransitions(basicStateData.getMachineStateJson().getId());

                    for (BasicTransitionData transition : stateTransitions) {

                        SketchFunction transitionFunction = transition.getSketchFunction();
                        SketchFunctionCall transitionCall = new SketchFunctionCall(transitionFunction);

                        SketchBooleanCondition booleanCondition = new SketchBooleanCondition(transitionCall);
                        SketchIfOperation checker = new SketchIfOperation(booleanCondition);

                        BasicStateData targetState = machineProcessor.getState(transition.getMachineTransitionJson().getTarget());
                        checker.addInstruction(currentState.redefineVariable(targetState.getSketchDefineDirective()));
                        basicStateData.getSketchFunction().addInstruction(checker);

                        if (resetTimer != null) {

                            SketchFunctionCall callReset = new SketchFunctionCall(resetTimer);
                            checker.addInstruction(callReset);

                        }

                    }

                    if (stateActivityJson.getEndPhase().size() > 0) {

                        SketchDefineDirective<Integer> defineDirective = getStructure().getPreprocessor(stateActivityJson.getState());

                        SketchIntegerCondition endIntegerCondition = new SketchIntegerCondition(currentState, defineDirective, SketchIntegerComparatorType.NOT_EQUALS);
                        SketchIfOperation endPhaseIfOperation = new SketchIfOperation(endIntegerCondition);

                        for (CommonPhaseJson endPhase : stateActivityJson.getEndPhase())
                            endPhaseIfOperation.addInstruction(new SketchFunctionCall(getStructure().getFunction(endPhase.getActivity())));

                        basicStateData.getSketchFunction().addInstruction(endPhaseIfOperation);

                    }

                    break;

                default:

                    break;

            }

        }

        for (ReadPortJson readPortJson : readPortJsons) {

            /*ReadActionData readActionData = getReadAction(readPortJson.getReadAction());
            TimerData timerData = getTimer(readPortJson.getReadPort());

            if (timerData != null) {

                readActionData.getSketchFunction().setResultInstruction(timerData.getSketchLongVariable().operateVariable(Long.valueOf("0"), SketchNumberOperator.SELF_MILLIS, new Object[0]));
                continue;

            }

            SketchVariable variable = getStructure().getVariable(readPortJson.getReadPort());

            if (variable != null) {

                if (variable instanceof SketchStringVariable)
                    readActionData.getSketchFunction().setReturnType(SketchFunctionType.STRING);

                readActionData.getSketchFunction().setResultVariable(variable);
                continue;

            }

            SketchLibraryVariable sketchLibraryVariable = getStructure().getLibraryVariable(readPortJson.getReadPort());

            if (sketchLibraryVariable != null) {

                if (sketchLibraryVariable instanceof SketchKeypadVariable) {

                    SketchKeypadVariable sketchKeypadVariable = (SketchKeypadVariable) sketchLibraryVariable;
                    readActionData.getSketchFunction().addInstruction(sketchKeypadVariable.operateReadAsResult());
                    continue;

                }

                readActionData.getSketchFunction().setResultLibraryVariable(sketchLibraryVariable);
                continue;

            }*/

        }

        for (PredicateVariablesJson predicateReadJson : predicateVariablesJsons) {

            PredicateJson predicateJson = getPredicate(predicateReadJson.getPredicate());

            PredicateComponentType operatorType = predicateJson.getOperator();

            SketchFunction predicateFunction = new SketchFunction("predicate_" + predicateJson.getLabel(), SketchFunctionType.BOOLEAN, false);

            SketchVariable v1 = getStructure().getVariable(predicateReadJson.getPrimaryVariable());
            SketchVariable v2 = getStructure().getVariable(predicateReadJson.getSecondaryVariable());

            //todo: se necesita SketchFloatCondition por que los valores son decimales ¿y si son string variables que?
            SketchIntegerVariable primaryVar=new  SketchIntegerVariable(v1.getLabel(), ((Double) v1.getValue()).intValue());
            SketchIntegerVariable secondaryVar=new  SketchIntegerVariable(v2.getLabel(), ((Double) v2.getValue()).intValue());

            SketchIntegerCondition condition = new SketchIntegerCondition(primaryVar, secondaryVar, operatorType.getComparatorType());
            predicateFunction.setResultOperation(condition);

            board.getSketch().addFunction(predicateFunction);
            getStructure().getOrPut(predicateJson.getId(), predicateFunction);

        }

        /*for (PredicateReadJson predicateReadJson : predicateReadJsons) {

            PredicateJson predicateJson = getPredicate(predicateReadJson.getPredicate());
            ReadActionData primaryAction = getReadAction(predicateReadJson.getReadActionPrimary());
            ReadActionData secondaryAction = getReadAction(predicateReadJson.getReadActionSecondary());

            PredicateComponentType operatorType = predicateJson.getOperator();

            SketchFunction predicateFunction = new SketchFunction("predicate_" + predicateJson.getLabel(), SketchFunctionType.BOOLEAN, false);

            SketchFunctionCall primaryCall = new SketchFunctionCall(primaryAction.getSketchFunction());
            SketchFunctionCall secondaryCall = new SketchFunctionCall(secondaryAction.getSketchFunction());

            SketchIntegerCondition condition = new SketchIntegerCondition(primaryCall, secondaryCall, operatorType.getComparatorType());
            predicateFunction.setResultOperation(condition);

            board.getSketch().addFunction(predicateFunction);
            getStructure().getOrPut(predicateJson.getId(), predicateFunction);

        }*/

        for (OperatorTransitionJson operatorTransitionJson : operatorTransitionJsons) {

            LinkedList<CommonPredicateJson> transitionPredicates = new LinkedList<>();

            BasicTransitionData transitionData = machineProcessor.getTransitionState(operatorTransitionJson.getTransition());
            LogicalOperatorJson loJson = getLogicalOperator(operatorTransitionJson.getLogicalOperator());

            for (OperatorPredicateJson operatorPredicateJson : operatorPredicateJsons)
                if (operatorPredicateJson.getLogicalOperator().equals(loJson.getId()))
                    transitionPredicates.addAll(operatorPredicateJson.getPredicates());

            SketchFunctionCall sketchFunctionCall = new SketchFunctionCall(getStructure().getFunction(getPredicate(transitionPredicates.get(0).getPredicate()).getId()));
            SketchBooleanCondition sketchBooleanCondition = new SketchBooleanCondition(sketchFunctionCall);

            transitionData.getSketchFunction().setResultOperation(sketchBooleanCondition);/**/

        }

        boolean hasContinousControls = false;

        for (WriteActionData writeActionData : writeActionDatas)
            for (ActionArgumentJson actionArgumentJson : writeActionData.getWriteActionJson().getArguments())
                for (ActionVariableJson actionVariables : actionVariableJsons)
                    if (actionArgumentJson.getId().equals(actionVariables.getActionArgument()))
                        actionArgumentJson.setVariableId(actionVariables.getVariable());

        for (ReadActionData writeActionData : readActionDatas)
            for (ActionArgumentJson actionArgumentJson : writeActionData.getReadActionJson().getArguments())
                for (ActionVariableJson actionVariables : actionVariableJsons)
                    if (actionArgumentJson.getId().equals(actionVariables.getActionArgument()))
                        actionArgumentJson.setVariableId(actionVariables.getVariable());

        for (ControlActionData controlActionData : controlActionDatas)
            for (ActionArgumentJson actionArgumentJson : controlActionData.getControlActionJson().getArguments())
                for (ActionVariableJson actionVariables : actionVariableJsons)
                    if (actionArgumentJson.getId().equals(actionVariables.getActionArgument()))
                        actionArgumentJson.setVariableId(actionVariables.getVariable());

        for (ControlActionData controlActionData : controlActionDatas) { // ControlActions continuous
            if(controlActionData.getControlActionJson().isContinuous()) {
                SketchDoubleVariable outputVariable = null;

                for(ActionResultJson actionResultJson : actionResultJsons)
                    if (actionResultJson.getAction().equals(controlActionData.getId())) {
                        outputVariable = getStructure().getVariable(actionResultJson.getVariable());
                        break;
                    }

                if(outputVariable == null)
                    break;

                if(!hasContinousControls) { // is continous?

                    SketchIncludeDirective includeDirective = new SketchHeaderPidLibrary();
                    board.getSketch().addPreprocessorDirective(includeDirective);
                    hasContinousControls = true;

                }

                ActionArgumentJson[] arguments = controlActionData.getControlActionJson().getArguments();
                ConfigurationArgumentJson[] configurations = controlActionData.getControlActionJson().getConfiguration();

                String suffix = "_" + controlActionData.getLabel();
                SketchPidVariable pidVariable = new SketchPidVariable("pid" + suffix, false, true);

                SketchDoubleVariable setpointVariable = getStructure().getVariable(arguments[0].getVariableId());
                SketchDoubleVariable inputVariable = getStructure().getVariable(arguments[1].getVariableId());

                SketchDoubleVariable kpVariable = new SketchDoubleVariable("ctl_conskp" + suffix, Double.parseDouble(configurations[0].getValue()));
                SketchDoubleVariable kiVariable = new SketchDoubleVariable("ctl_conski" + suffix, Double.parseDouble(configurations[1].getValue()));
                SketchDoubleVariable kdVariable = new SketchDoubleVariable("ctl_conskd" + suffix, Double.parseDouble(configurations[2].getValue()));

                pidVariable.setInput(inputVariable);
                pidVariable.setOutput(outputVariable);
                pidVariable.addSetPoint(setpointVariable);

                pidVariable.setKp(kpVariable);
                pidVariable.setKi(kiVariable);
                pidVariable.setKd(kdVariable);

                board.getSketch().addVariables(kpVariable, kiVariable, kdVariable);
                board.getSketch().addLibraryVariable(pidVariable);

                board.getSketch().getSetupFunction().addInstruction(pidVariable.operateSetMode(SketchPidVariable.ModeType.AUTOMATIC));
                controlActionData.getSketchFunction().addInstruction(codeBuffer -> {
                    codeBuffer.appendLine(pidVariable.getLabel() + ".Compute();");
                    return false;
                });

                getStructure().getOrPut(outputVariable.getLabel(), outputVariable);
                getStructure().getOrPut(inputVariable.getLabel(), inputVariable);
                getStructure().getOrPut(setpointVariable.getLabel(), setpointVariable);

                getStructure().getOrPut(kpVariable.getLabel(), kpVariable);
                getStructure().getOrPut(kiVariable.getLabel(), kiVariable);
                getStructure().getOrPut(kdVariable.getLabel(), kdVariable);

                getStructure().getOrPut(controlActionData.getId(), pidVariable);

            }
        }

        for (DeviceActionJson deviceActionJson : deviceActionJsons) { // Write and Read

            DeviceData deviceData = null;

            WriteActionData writeActionData = null;
            ReadActionData readActionData = null;

            boolean isWriteAction = false;

            for (DeviceJson deviceJson : hardwareProcessor.getDeviceJsons())
                if (deviceActionJson.getDevice().equals(deviceJson.getId())) {
                    deviceData = new DeviceData(deviceProcessor, deviceJson);
                    break;
                }

            for (WriteActionData writeData : writeActionDatas)
                if (deviceActionJson.getAction().equals(writeData.getWriteActionJson().getId())) {
                    writeActionData = writeData;
                    isWriteAction = true;
                    break;
                }

            for (ReadActionData readData : readActionDatas)
                if (deviceActionJson.getAction().equals(readData.getReadActionJson().getId())) {
                    readActionData = readData;
                    isWriteAction = false;
                    break;
                }

            if (deviceData == null)
                continue;

            if ((isWriteAction && writeActionData == null) || (!isWriteAction && readActionData == null))
                continue;

            if (isWriteAction) {

                DeviceWriter writer = new DeviceWriter(this, hardwareProcessor);
                deviceProcessor.processDeviceMacro(deviceData, hardwareProcessor);
                deviceProcessor.processDeviceWrite(deviceData, writer, writeActionData);
                continue;

            }

            DeviceWriter writer = new DeviceWriter(this, hardwareProcessor);
            deviceProcessor.processDeviceMacro(deviceData, hardwareProcessor);
            deviceProcessor.processDeviceRead(deviceData, writer, readActionData);
            continue;

        }

    }

    public LinkedList<BindingComponent<?>> getBindingComponents(List<BindingComponentJson> bindingComponentJsons) {

        LinkedList<BindingComponent<?>> bindingComponents = new LinkedList<>();

        for (BindingComponentJson bindingComponentJson : bindingComponentJsons)
            bindingComponents.add(bindingComponentJson.getComponent());

        return bindingComponents;

    }

    public LogicalOperatorJson getLogicalOperator(String id) {

        for (LogicalOperatorJson json : logicalOperatorJsons)
            if (json.getId().equals(id))
                return json;

        return null;

    }

    public PredicateJson getPredicate(String id) {

        for (PredicateJson json : predicateJsons)
            if (json.getId().equals(id))
                return json;

        return null;

    }

    public ControlActionData getControlAction(String id) {

        for (ControlActionData data : controlActionDatas)
            if (data.getControlActionJson().getId().equals(id))
                return data;

        return null;

    }

    public WriteActionData getWriteAction(String id) {

        for (WriteActionData data : writeActionDatas)
            if (data.getWriteActionJson().getId().equals(id))
                return data;

        return null;

    }

    public ReadActionData getReadAction(String id) {

        for (ReadActionData data : readActionDatas)
            if (data.getReadActionJson().getId().equals(id))
                return data;

        return null;

    }

    public ReadActionData getVariable(String id) {

        for (ReadActionData data : readActionDatas)
            if (data.getReadActionJson().getId().equals(id))
                return data;

        return null;

    }

    public TimerData getTimer(String id) {

        for (TimerData data : timerDatas)
            if (data.getTimerJson().getId().equals(id))
                return data;

        return null;

    }

    public ActivityJson getActivity(String id) {

        for (ActivityJson activityJson : activityJsons)
            if (activityJson.getId().equals(id))
                return activityJson;

        return null;

    }

}
