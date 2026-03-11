package com.ruben.bblib.api.molang;

@FunctionalInterface
public interface MolangValue {

    double evaluate(MolangContext context);
}

