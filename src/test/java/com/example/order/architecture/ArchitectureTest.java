package com.example.order.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit 테스트 - 헥사고날 아키텍처 규칙 검증
 *
 * 레이어 의존성 규칙:
 * - Domain은 어디에도 의존하지 않음 (순수 비즈니스 로직)
 * - Application은 Domain에만 의존
 * - Infrastructure는 Domain과 Application에 의존
 *
 * 패키지 의존성 규칙:
 * - Domain 패키지는 Spring Framework 의존 금지
 * - Application 패키지는 Infrastructure 직접 참조 금지
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
                // Allow spring annotation for specific purposes if needed, 
                // but generally it should be framework-free.
                .check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnInfrastructure() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .check(importedClasses);
    }

    @Test
    void applicationShouldNotDependOnInfrastructure() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
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
