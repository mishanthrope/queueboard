package com.mishanthrope.queueboard;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

    private static final ApplicationModules MODULES =
            ApplicationModules.of(QueueboardApplication.class);

    @Test
    void modules_respectTheirBoundaries() {
        MODULES.verify();
    }

    @Test
    void documentation_isWrittenForTheModules() {
        new Documenter(MODULES)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
