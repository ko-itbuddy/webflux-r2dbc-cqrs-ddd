package com.example.order.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit 테스트 - 헥사고날 아키텍처 규칙 검증 (Practical Hexagonal)
 * 
 * 레이어 의존성 규칙:
 * - Domain: 핵심 비즈니스 로직. JPA 어노테이션 허용 (Practical approach).
 * - Application: 비즈니스 유스케이스 및 조율.
 * - Adapter (In/Out): 외부 시스템과의 연동.
 */
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.example");
    }

    @Test
    void layeredArchitectureShouldBeRespected() {
        Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInAnyPackage("com.example..")
                .layer("Common").definedBy("com.example.common..")
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("AdapterIn").definedBy("..adapter.in..")
                .layer("AdapterOut").definedBy("..adapter.out..")
                .layer("Config").definedBy("..adapter.config..")

                .whereLayer("Domain").mayOnlyAccessLayers("Common")
                .whereLayer("Application").mayOnlyAccessLayers("Domain", "Common")
                .whereLayer("AdapterIn").mayOnlyAccessLayers("Application", "Domain", "Common")
                .whereLayer("AdapterOut").mayOnlyAccessLayers("Application", "Domain", "Common")
                .whereLayer("Config").mayOnlyAccessLayers("Application", "Domain", "AdapterIn", "AdapterOut", "Common")

                .check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnSpringFramework() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..")
                .check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnAdapters() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .check(importedClasses);
    }

    @Test
    void applicationShouldNotDependOnAdapters() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    void domainShouldNotHaveCycleDependencies() {
        SlicesRuleDefinition.slices()
                .matching("com.example.(*).domain.(*)..")
                .should()
                .beFreeOfCycles()
                .check(importedClasses);
    }

    @Test
    void applicationShouldNotHaveCycleDependencies() {
        SlicesRuleDefinition.slices()
                .matching("com.example.(*).application.(*)..")
                .should()
                .beFreeOfCycles()
                .check(importedClasses);
    }
}