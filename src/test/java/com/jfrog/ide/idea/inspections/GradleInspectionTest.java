package com.jfrog.ide.idea.inspections;

import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;

/**
 * @author yahavi
 */
public class GradleInspectionTest extends InspectionsTest {

    private final Object[][] DEPENDENCIES = {
            // offset, groupId, artifactId
            {96, "a", "b"},
            {139, "a", "b"},
            {180, "a", "b"},
            {215, "d", "e"},
            {256, "a", "b"},
            {321, "", "project"}
    };

    private final int[] NON_DEPENDENCIES_POSITIONS = {20, 287, 385};

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public void setUp() throws Exception {
        super.setUp(new GradleInspection(), GrLiteral.class);
    }

    public void testDependencies() {
        isDependencyTest(DEPENDENCIES);
    }

    public void testNonDependencies() {
        isNonDependencyTest(NON_DEPENDENCIES_POSITIONS);
    }

    public void testCreateGeneralInfo() {
        createGeneralInfoTest(DEPENDENCIES);
    }
}
