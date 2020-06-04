package com.variamos.moduino.binder.api.json.binding.actions;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class ActionArgumentJson {

    private String id;
    private String label;
    private String type;
    private String dataType; // todo cambio para pruebas

    private String variableId;

    public void setVariableId(String id) {
        this.variableId = id;
    }

}
