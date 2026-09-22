package org.akira.ladux.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Module ownership and conventional Controller -> Service -> Repository layering. */
class ArchitectureRuleTest {
    private static final String ROOT = "org.akira.ladux";
    private static final Set<String> MODULES = Set.of("identity", "customer", "catalog", "ordering",
            "payment", "inventory", "procurement", "promotion", "notification", "assistant", "shared");
    private static final Set<String> LAYERS = Set.of("controller", "service", "repository", "mapper",
            "dto", "entity", "enums", "config", "security", "util", "event", "listener", "exception", "scheduling");
    private static final JavaClasses MAIN = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    private static final ArchRule CONTROLLERS_USE_SERVICES = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().areAssignableTo(Repository.class);

    @Test
    void everyClassBelongsToAnExplicitModuleAndLayer() {
        assertFalse(MAIN.isEmpty());
        for (String module : MODULES) {
            assertTrue(MAIN.stream().anyMatch(type -> type.getPackageName().startsWith(ROOT + "." + module + ".")),
                    "Missing module: " + module);
        }
        classes().should(new ArchCondition<JavaClass>("belong to a module and a conventional layer") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                if (type.getName().equals(ROOT + ".LaduxApplication")) return;
                String[] parts = type.getPackageName().substring(ROOT.length() + 1).split("\\.");
                boolean valid = parts.length >= 2 && MODULES.contains(parts[0]) && LAYERS.contains(parts[1]);
                events.add(new SimpleConditionEvent(type, valid, type.getName() + " must belong to module.layer"));
            }
        }).check(MAIN);
    }

    @Test
    void controllersAndRepositoriesHaveClearResponsibilities() {
        classes().that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..controller..").check(MAIN);
        classes().that().areAssignableTo(Repository.class)
                .should().resideInAPackage("..repository..").check(MAIN);
        classes().that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..entity..").check(MAIN);
        CONTROLLERS_USE_SERVICES.check(MAIN);
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAnyPackage("..repository..", "jakarta.persistence..",
                        "org.springframework.jdbc..", "org.springframework.data.jpa..").check(MAIN);
        noClasses().that().resideInAPackage("..repository..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..", "..mapper..")
                .check(MAIN);
    }

    @Test
    void servicesDoNotDependOnHttpControllers() {
        noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..").check(MAIN);
    }

    @Test
    void dtoAndEntityDoNotContainServiceOrPersistenceAccess() {
        noClasses().that().resideInAPackage("..dto..")
                .should().dependOnClassesThat().resideInAnyPackage("..entity..", "..repository..", "..service..",
                        "..controller..", "..mapper..").check(MAIN);
        noClasses().that().resideInAPackage("..entity..")
                .should().dependOnClassesThat().resideInAnyPackage("..repository..", "..service..", "..controller..",
                        "..mapper..", "..dto..").check(MAIN);
        noClasses().that().resideInAPackage("..mapper..")
                .should().dependOnClassesThat().resideInAnyPackage("..repository..", "..service..", "..controller..")
                .check(MAIN);
    }

    @Test
    void sharedDoesNotDependOnBusinessModules() {
        String[] businessPackages = MODULES.stream().filter(module -> !module.equals("shared"))
                .map(module -> ROOT + "." + module + "..").toArray(String[]::new);
        noClasses().that().resideInAPackage(ROOT + ".shared..")
                .should().dependOnClassesThat().resideInAnyPackage(businessPackages).check(MAIN);
    }

    @Test
    void controllerRuleDetectsRepositoryAccessEvenWithoutRepositorySuffix() {
        JavaClasses fixture = new ClassFileImporter().importClasses(BadController.class, DataAccess.class);
        assertTrue(CONTROLLERS_USE_SERVICES.evaluate(fixture).hasViolation());
    }

    interface DataAccess extends Repository<Object, Integer> {
    }

    @RestController
    static class BadController {
        private final DataAccess dataAccess;

        BadController(DataAccess dataAccess) {
            this.dataAccess = dataAccess;
        }
    }
}
