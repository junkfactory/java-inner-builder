<!-- Plugin description -->

# An opinionated Java Inner Builder Generator

This is an opinionated but simple Java Inner Builder Generator IntelliJ plugin that generates an
inner builder for a given class.

Based from [InnerBuilder](https://github.com/analytically/innerbuilder) with stripped down features.

1. Generates builder method for final fields that are not initialized, static fields are excluded
2. Generates static `builder()` method inside the parent class
3. Uses field names as setters in the builder
4. Detects collection types and generates `addTo...` method for them if they are initialized

Optional features:

1. Generates `toBuilder()` method to convert the object to a builder
2. Generates `validate()` method to validate the fields before building the object

### Example

Initial

```java
public class Person {
    static String m = "ME";
    private static final Logger logger = Logger.getLogger("test");
    private final String name;
    private final int age;
    private String lastName;

    private List<String> list;

    private Set<String> set;

    private final List<Address> addresses = new ArrayList<>();

}
```

Generates

```java
public class Person {
    static String m = "ME";
    private static final Logger logger = Logger.getLogger("test");
    private final String name;
    private final int age;
    private String lastName;

    private List<String> list;

    private Set<String> set;

    private final List<Address> addresses;

    private Person(Builder builder) {
        name = builder.name;
        age = builder.age;
        lastName = builder.lastName;
        list = builder.list;
        set = builder.set;
        addresses = builder.addresses;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.name = this.name;
        builder.age = this.age;
        builder.lastName = this.lastName;
        builder.list = this.list;
        builder.set = this.set;
        builder.addresses = this.addresses;
        return builder;
    }

    public static final class Builder {
        private String name;
        private int age;
        private String lastName;
        private List<String> list;
        private Set<String> set;
        private List<Address> addresses = new ArrayList<>();

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder list(List<String> list) {
            this.list = list;
            return this;
        }

        public Builder set(Set<String> set) {
            this.set = set;
            return this;
        }

        public Builder addToAddresses(Address e) {
            this.addresses.add(e);
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
record Address(Person person, List<String> streets, String city) {
    static String m = "ME";

}
```

Generates

```java
record Address(Person person, List<String> streets, String city) {
    static String m = "ME";

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.person = this.person;
        builder.streets = this.streets;
        builder.city = this.city;
        return builder;
    }

    public static final class Builder {
        private Person person;
        private List<String> streets;
        private String city;

        private Builder() {
        }

        public Builder person(Person person) {
            this.person = person;
            return this;
        }

        public Builder streets(List<String> streets) {
            this.streets = streets;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        private void validate() {
        }

        Address build() {
            validate();
            return new Address(person, streets, city);
        }
    }
}
```

<!-- Plugin description end -->

## Installation

1. Download plugin zip file from [Releases](https://github.com/junkfactory/java-inner-builder/releases)
2. Open IntelliJ IDEA, go to `File -> Settings -> Plugins`
3. Click on the gear icon and select `Install Plugin from Disk...`
4. Select the downloaded zip file and click `OK`