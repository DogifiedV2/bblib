package com.ruben.bblib.internal;

import com.ruben.bblib.example.TestEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BBLibCommon {
    public static final String MOD_ID = "bblib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private BBLibCommon() {
    }

    public static void init() {
        TestEntities.register();
        LOGGER.info("BBLib initialized");
    }
}

