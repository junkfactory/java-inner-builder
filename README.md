# An opinionated Java Inner Builder Generator

This is a simple Java Inner Builder Generator IntelliJ plugin that generates a
builder for a given class. The builder is an inner class of the class it is building.

Automatically detects the target class access modifier

```java
public class Person {
    private static final Logger logger = Logger.getLogger("test");

    private final String name = "1";
    private final int age;
    private String lastName;

    private Person(Builder builder) {
        age = builder.age;
        lastName = builder.lastName;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {
        private int age;
        private String lastName;

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Person build() {
            return new Person(this);
        }
    }
}

```

Supports Java record classes

```java
record Address(String street, String city, String state, String country) {
    static String m = "ME";

    public static Builder builder() {
        return new Builder();
    }

    //optional toBuilder method
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.street = this.street;
        builder.city = this.city;
        builder.state = this.state;
        builder.country = this.country;
        return builder;
    }

    public static final class Builder {
        private String street;
        private String city;
        private String state;
        private String country;

        public Builder street(String street) {
            this.street = street;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        //follows the containing class visibility
        Address build() {
            return new Address(street, city, state, country);
        }
    }
}
```