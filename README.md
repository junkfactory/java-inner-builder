<!-- Plugin description -->

# An opinionated Java Inner Builder Generator

This is an opinionated but simple Java Inner Builder Generator IntelliJ plugin that generates a
builder for a given class. The builder is an inner class of the class it is building.

Based from [InnerBuilder](https://github.com/analytically/innerbuilder) with stripped down features.

Generates a builder class for a given class with the following features:

1. Generates builder method for final fields that are not initialized and/or static fields
2. Generates static `builder()` method inside the parent class
3. Uses field names as setters in the builder

Optional features:

1. Generates `toBuilder()` method to convert the object to a builder
2. Generates `validate()` method to validate the fields before building the object

<!-- Plugin description end -->

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

        private Builder() {
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        private void validate() {
        }

        public Person build() {
            validate();
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

        private Builder() {
        }

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

## Installation

1. Download plugin zip file from [Releases](https://github.com/junkfactory/java-inner-builder/releases)
2. Open IntelliJ IDEA, go to `File -> Settings -> Plugins`
3. Click on the gear icon and select `Install Plugin from Disk...`
4. Select the downloaded zip file and click `OK`