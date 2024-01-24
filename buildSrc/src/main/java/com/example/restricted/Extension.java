package com.example.restricted;

import com.h0tk3y.kotlin.staticObjectNotation.Adding;
import com.h0tk3y.kotlin.staticObjectNotation.Configuring;
import com.h0tk3y.kotlin.staticObjectNotation.Restricted;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

// Used on a project extension type, this annotation tells the restricted DSL interpreter to include the extension
// in the schema that it builds for the project
@Restricted
public abstract class Extension {
    private final Access primaryAccess;
    
    // Normal properties that have no annotations are not visible in the restricted DSL.
    public abstract ListProperty<Access> getSecondaryAccess();
    
    private final ObjectFactory objects;

    public Access getPrimaryAccess() {
        return primaryAccess;
    }

    @Inject
    public Extension(ObjectFactory objects) {
        this.objects = objects;
        this.primaryAccess = objects.newInstance(Access.class);
        this.primaryAccess.getName().set("primary");
        
        getId().convention("<no id>");
        getReferencePoint().convention(point(-1, -1));
    }

    // Properties annotated with @Restricted are included in the schema. They, however, can only be assigned using
    // the assign operator (`=`), and neither `.set(...)` nor any other Property<T> APIs are available in the DSL. 
    @Restricted
    public abstract Property<String> getId();

    // If a @Restricted Property<T> has a custom type, it will also be included in the schema, which is necessary
    // if these properties get their values assigned from factory function calls.
    @Restricted
    public abstract Property<Point> getReferencePoint();

    // This annotation explains the semantics of invocations: all of them configure the same object (rather than 
    // create a new object on each invocation). 
    // It is a replacement for @Restricted that is more concrete about semantics.
    // The schema builder treats Gradle Action<...> as the configuring function type. 
    // Kotlin function types are supported, too.
    @Configuring
    public void primaryAccess(Action<? super Access> configure) {
        configure.execute(primaryAccess);
    }

    // Contrary to @Configuring, an @Adding create a new element in this container each time it is invoked.
    @Adding
    public Access secondaryAccess(Action<? super Access> configure) {
        Access newAccess = objects.newInstance(Access.class);
        newAccess.getName().convention("<no name>");
        configure.execute(newAccess);
        getSecondaryAccess().add(newAccess);
        return newAccess;
    }

    // This function is only supposed to return a value (behaves like a "pure" function), so it is neither @Adding
    // nor @Configuring. However, it must still be annotated as @Restricted to be included in the schema.
    // As this function has no side effects, using it as a statement (ignoring the return value) is not allowed 
    // in the restricted DSL.
    @Restricted
    public Point point(int x, int y) {
        return new Point(x, y);
    }

    public abstract static class Access {
        public Access() {
            getName().convention("<no name>");
            getRead().convention(false);
            getWrite().convention(false);
        }
        
        @Restricted
        public abstract Property<String> getName();

        @Restricted
        public abstract Property<Boolean> getRead();

        @Restricted
        public abstract Property<Boolean> getWrite();
    }

    public static class Point {
        public final int x;
        public final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
