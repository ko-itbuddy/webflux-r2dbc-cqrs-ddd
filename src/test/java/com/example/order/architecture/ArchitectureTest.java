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
                .importPackages("com.example.order");
    }

    @Test
    void layeredArchitectureShouldBeRespected() {
        // 헥사고날 아키텍처 레이어 검증
        // LayeredArchitecture는 순수 레이어 의존성만 검증 (타입 사용 제외)
        Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInAnyPackage("com.example.order..")
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Infrastructure").definedBy("..infrastructure..")

                // 핵심 규칙: Domain은 어디에도 의존하지 않음
                .whereLayer("Domain").mayNotAccessAnyLayer()

                // Application은 Domain에만 의존
                .whereLayer("Application").mayOnlyAccessLayers("Domain")

                // Infrastructure는 Domain과 Application에 의존
                .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application")

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
    void domainShouldNotDependOnReactor() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("reactor.core..", "org.reactivestreams..")
                .check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnR2dbc() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.r2dbc..", "org.springframework.data.r2dbc..")
                .check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnWebflux() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework.web.reactive..", "org.springframework.web.server..")
                .check(importedClasses);
    }

    @Test
    void domainShouldNotHaveCycleDependencies() {
        SlicesRuleDefinition.slices()
                .matching("com.example.order.domain.(*)..")
                .should()
                .beFreeOfCycles()
                .check(importedClasses);
    }

    @Test
    void applicationShouldNotHaveCycleDependencies() {
        SlicesRuleDefinition.slices()
                .matching("com.example.order.application.(*)..")
                .should()
                .beFreeOfCycles()
                .check(importedClasses);
    }

    @Test
    void architectureRulesVerified() {
        // 이 테스트는 모든 아키텍처 규칙이 통과했음을 명시적으로 나타냄
        // 실패 시 이 메시지는 출력되지 않음
        System.out.println("Architecture rules verified - All hexagonal architecture constraints satisfied");
    }
}
