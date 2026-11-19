package com.mishanthrope.queueboard;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.mishanthrope.queueboard",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "com.mishanthrope.queueboard.";
    private static final String CORE_DOMAIN = ROOT + "core.domain..";
    private static final String CORE_APPLICATION = ROOT + "core.application..";
    private static final String CORE_INFRASTRUCTURE = ROOT + "core.infrastructure..";
    private static final String CORE_PRESENTATION = ROOT + "core.presentation..";
    private static final String CORE = ROOT + "core..";
    private static final String CORE_EVENTS = ROOT + "core.events..";
    private static final String ANALYTICS = ROOT + "analytics..";
    private static final String ANALYTICS_DOMAIN = ROOT + "analytics.domain..";
    private static final String ANALYTICS_APPLICATION = ROOT + "analytics.application..";
    private static final String ANALYTICS_INFRASTRUCTURE = ROOT + "analytics.infrastructure..";
    private static final String ANALYTICS_PRESENTATION = ROOT + "analytics.presentation..";
    private static final String ANALYTICS_ACL = ROOT + "analytics.infrastructure.acl..";

    // Core layers

    @ArchTest
    static final ArchRule coreDomainDependsOnlyOnJavaAndItself = classes()
            .that().resideInAPackage(CORE_DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", CORE_DOMAIN);

    @ArchTest
    static final ArchRule coreApplicationDoesNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage(CORE_APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(CORE_INFRASTRUCTURE);

    @ArchTest
    static final ArchRule coreApplicationDoesNotDependOnPresentation = noClasses()
            .that().resideInAPackage(CORE_APPLICATION)
            .should().dependOnClassesThat().resideInAPackage(CORE_PRESENTATION);

    @ArchTest
    static final ArchRule corePresentationDoesNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage(CORE_PRESENTATION)
            .should().dependOnClassesThat().resideInAPackage(CORE_INFRASTRUCTURE);

    @ArchTest
    static final ArchRule coreInfrastructureDoesNotDependOnPresentation = noClasses()
            .that().resideInAPackage(CORE_INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAPackage(CORE_PRESENTATION);

    @ArchTest
    static final ArchRule corePresentationDoesNotUseDomainRepositories = noClasses()
            .that().resideInAPackage(CORE_PRESENTATION)
            .should().dependOnClassesThat().haveFullyQualifiedName(
                    ROOT + "core.domain.order.OrderRepository")
            .orShould().dependOnClassesThat().haveFullyQualifiedName(
                    ROOT + "core.domain.employee.EmployeeRepository");

    // Analytics layers

    @ArchTest
    static final ArchRule analyticsDomainDependsOnlyOnJavaAndItself = classes()
            .that().resideInAPackage(ANALYTICS_DOMAIN)
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("java..", ANALYTICS_DOMAIN);

    @ArchTest
    static final ArchRule analyticsApplicationDoesNotDependOnInfrastructureOrPresentation =
            noClasses()
                    .that().resideInAPackage(ANALYTICS_APPLICATION)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            ANALYTICS_INFRASTRUCTURE, ANALYTICS_PRESENTATION);

    @ArchTest
    static final ArchRule analyticsPresentationDoesNotDependOnInfrastructureOrDomain =
            noClasses()
                    .that().resideInAPackage(ANALYTICS_PRESENTATION)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            ANALYTICS_INFRASTRUCTURE, ANALYTICS_DOMAIN);

    @ArchTest
    static final ArchRule analyticsInfrastructureDoesNotDependOnPresentation = noClasses()
            .that().resideInAPackage(ANALYTICS_INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAPackage(ANALYTICS_PRESENTATION);

    // Between modules

    @ArchTest
    static final ArchRule nothingInCoreDependsOnAnalytics = noClasses()
            .that().resideInAPackage(CORE)
            .should().dependOnClassesThat().resideInAPackage(ANALYTICS);

    @ArchTest
    static final ArchRule onlyTheAclDependsOnCoreEvents = noClasses()
            .that().resideInAPackage(ANALYTICS)
            .and().resideOutsideOfPackage(ANALYTICS_ACL)
            .should().dependOnClassesThat().resideInAPackage(CORE_EVENTS);

    @ArchTest
    static final ArchRule analyticsDependsOnCoreOnlyThroughEvents = noClasses()
            .that().resideInAPackage(ANALYTICS)
            .should().dependOnClassesThat().resideInAnyPackage(
                    CORE_DOMAIN, CORE_APPLICATION, CORE_INFRASTRUCTURE, CORE_PRESENTATION);
}
