package com.ruben.bblib.internal.parser;

import com.ruben.bblib.api.model.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ParseResult {

    private final String modelId;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    @Nullable
    private ModelData model;

    public ParseResult(String modelId) {
        this.modelId = modelId;
    }

    public void warn(String message) {
        warnings.add(message);
    }

    public void error(String message) {
        errors.add(message);
    }

    public String getModelId() {
        return modelId;
    }

    @Nullable
    public ModelData getModel() {
        return model;
    }

    public void setModel(ModelData model) {
        this.model = model;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}

