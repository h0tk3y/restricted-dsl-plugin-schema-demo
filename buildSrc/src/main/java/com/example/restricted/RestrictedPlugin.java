package com.example.restricted;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public class RestrictedPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        // Register the extension normally:
        Extension restricted = target.getExtensions().create("restricted", Extension.class);

        // Use the data from the extension at execution time:
        target.getTasks().register("printConfiguration", DefaultTask.class, task -> {
            Property<Extension.Point> referencePoint = restricted.getReferencePoint();
            Extension.Access acc = restricted.getPrimaryAccess();
            ListProperty<Extension.Access> secondaryAccess = restricted.getSecondaryAccess();

            task.doLast("print restricted extension content", t -> {
                System.out.println("id = " + restricted.getId().get());
                Extension.Point point = referencePoint.getOrElse(restricted.point(-1, -1));
                System.out.println("referencePoint = " + point.x + ", " + point.y);
                System.out.println("primaryAccess = { " +
                        acc.getName().get() + ", " + acc.getRead().get() + ", " + acc.getWrite().get() + "}"
                );
                secondaryAccess.get().forEach(it -> System.out.println(
                        "secondaryAccess { " + 
                                it.getName().get() + ", " + it.getRead().get() + ", " + it.getWrite().get() + "}"
                ));
            });
        });
    }
}
