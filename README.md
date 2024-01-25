# Restricted DSL plugin schema demo

This repository demonstrates contributing to a restricted DSL schema from third-party Gradle plugins. See:

* [`buildSrc/src/main/java/.../Extension.java`](buildSrc/src/main/java/com/example/restricted/Extension.java) 
  for the annotated project extension;
  * Currently, the extension is registered with regular Gradle [plugin code](https://github.com/h0tk3y/restricted-dsl-plugin-schema-demo/blob/master/buildSrc/src/main/java/com/example/restricted/RestrictedPlugin.java#L13). The registration routine is subject to future changes.
* [`build.gradle.something`](build.gradle.something) showing the usage of the plugin in the restricted DSL.

## How it works

When Gradle builds a restricted DSL schema for the project build file, it checks the project extensions and includes
the ones with types annotated as `@Restricted` in the schema next to the built-in schema parts.

> The rules and APIs for describing the schema are going to change.

Supported members in restricted types:

* `Property<T>`-typed properties. These must be annotated with `@Restricted` as well to be included in the schema.

  ```java
  @Restricted
  public abstract Property<String> getId();
  ```

  If such a property has a user-defined type, the type is also included in the schema.

  In a declarative file, these properties may only be assigned using the `=` operator.
  
  ```kotlin
  id = "test"
  ```
  
  None of the other Gradle `Proprerty<T>` APIs are available in the restricted DSL.

* Annotated functions that accept a configuring code block (lambda). 
  These must have the last parameter of type `Action<? super T>` or the Kotlin function type `T.() -> Unit`.
  
  The configured type of such a function is also included in the schema.

  These functions must explain their semantics with one of these two annotations:

  * `@Configuring`, if the function configures an existing object.
  * `@Adding`, if the function creates and configures a new object each time when it is invoked.
  
  Currently, they must also provide an implementation to work at runtime, and the contract is not checked.
  For the restricted DSL runtime to correctly apply the data to the JVM objects, the implementation must invoke
  the configuring lambda passing it the configured object.

  ```java
  @Configuring
  public void primaryAccess(Action<? super Access> configure) {
      configure.execute(primaryAccess);
  }
  
  @Adding
  public Access secondaryAccess(Action<? super Access> configure) {
      Access newAccess = objects.newInstance(Access.class);
      newAccess.getName().convention("<no name>");
      configure.execute(newAccess);
      getSecondaryAccess().add(newAccess);
      return newAccess;
  }
  ```
  
  In the DSL:

  ```kotlin
  restricted {
      primaryAccess {
          name = "admin"
      }
      secondaryAccess {
          name = "user1" 
      }
  }
  ```

* Factory functions producing values of types that are included in the schema.

  These functions must be annotated with `@Restricted` and are supposed to be pure.

  ```java
  @Restricted
  public Point point(int x, int y) {
       return new Point(x, y);
  }
  ```
  
  Parameters of other custom types included in the schema are accepted, but as of now, parameter types of such functions 
  are not automatically included in the schema. This is going to change. 
  
  Invocations of these functions may appear as values in the DSL but are not allowed in a position of a statement 
  (i.e. as dangling pure expressions).

  ```kotlin
  referencePoint = point(42, 42)
  ```

## Interpretation result example

Once Gradle builds the schema, it is exported and can be used externally to interpret the declarative files 
(see https://github.com/h0tk3y/external-schema-demo/). An example output would be:

```text
ProjectTopLevelReceiver#0 {
    .projectExtension:restricted Extension#0 from (top-level-object).projectExtension:restricted {
        id = "test"
        referencePoint = point#4(
            arg0 = 1
            arg1 = 2
        )
        + added Access#9 from (top-level-object).projectExtension:restricted.secondaryAccess#9() {
            name = "two"
            read = true
            write = false
        }
        + added Access#13 from (top-level-object).projectExtension:restricted.secondaryAccess#13() {
            name = "three"
            read = true
            write = true
        }
        .primaryAccess{} Access#0 from (top-level-object).projectExtension:restricted.primaryAccess#6(){}-receiver {
            read = false
            write = false
        }
    }
}
```
