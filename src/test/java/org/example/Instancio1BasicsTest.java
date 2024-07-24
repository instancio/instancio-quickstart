package org.example;

import org.example.generics.Pair;
import org.example.person.Address;
import org.example.person.Gender;
import org.example.person.Person;
import org.example.person.Phone;
import org.example.person.PhoneWithExtension;
import org.instancio.Assign;
import org.instancio.Assignment;
import org.instancio.Instancio;
import org.instancio.Result;
import org.instancio.When;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.all;
import static org.instancio.Select.field;

/**
 * This class demonstrates basic usage of the API:
 *
 * <ul>
 *   <li>create() to create objects</li>
 *   <li>asResult() to get seed value</li>
 *   <li>set() and generate() to customise objects</li>
 *   <li>onComplete() callbacks</li>
 *   <li>subtype() to specify a subclass</li>
 *   <li>ignore() to ignore fields/classes</li>
 *   <li>withNullable() to allow null values to be generated</li>
 *   <li>withUnique() for generating unique vaalues</li>
 *   <li>assign() to set values based on another generated value</li>
 *   <li>cartesianProduct() to generate the Cartesian product for given values</li>
 *   <li>filter() to filter generated values</li>
 *   <li>setBlank() to generate blank POJOs with null fields</li>
 * </ul>
 */
class Instancio1BasicsTest {

    @Test
    @DisplayName("Create random values; by default, generates positive numbers and non-empty strings")
    void randomValues() {
        int i = Instancio.create(int.class);
        String str = Instancio.create(String.class);
        LocalDateTime ldt = Instancio.create(LocalDateTime.class);

        assertThat(i).isPositive();
        assertThat(str).isNotBlank();
        assertThat(ldt).isNotNull();
    }

    @Test
    @DisplayName("Create a fully-populated object")
    void fullyPopulateObject() {
        Phone phone = Instancio.create(Phone.class);

        assertThat(phone.getCountryCode()).isNotNull();
        assertThat(phone.getNumber()).isNotNull();
    }

    @Test
    @DisplayName("asResult() can be used when seed value is needed, e.g. for logging")
    void asResult() {
        Result<Phone> result = Instancio.of(Phone.class).asResult();
        Phone phone = result.get();

        assertThat(phone).isNotNull();
        System.out.println("Phone was created using seed: " + result.getSeed());
    }

    @Test
    @DisplayName("generate() is for customising randomly generated values of string, numbers, dates, etc")
    void customiseObjectUsingGenerate() {
        Phone phone = Instancio.of(Phone.class)
                .generate(field(Phone::getCountryCode), gen -> gen.oneOf("+1", "+44"))
                .generate(field(Phone::getNumber), gen -> gen.string().digits().length(7))
                .create();

        assertThat(phone.getNumber()).containsOnlyDigits().hasSize(7);
        assertThat(phone.getCountryCode()).isIn("+1", "+44");
    }

    @Test
    @DisplayName("set() is for setting non-random (expected) values")
    void customiseObjectUsingSet() {
        Phone phone = Instancio.of(Phone.class)
                .set(field(Phone::getCountryCode), "+1")
                .create();

        assertThat(phone.getCountryCode()).isEqualTo("+1");
    }

    @Test
    @DisplayName("onComplete() callback is invoked after object has been fully populated")
    void oncComplete() {
        Person person = Instancio.of(Person.class)
                .onComplete(all(Person.class), (Person p) -> {
                    String name = p.getGender() == Gender.FEMALE ? "Marge" : "Homer";
                    p.setFirstName(name);
                })
                .create();

        assertThat(person.getFirstName()).isIn("Marge", "Homer");
    }

    @Test
    @DisplayName("subtype() allows specifying implementations for abstract types, or subclasses for concrete types")
    void usingSubtype() {
        Address address = Instancio.of(Address.class)
                .subtype(all(Phone.class), PhoneWithExtension.class)
                .create();

        assertThat(address.getPhoneNumbers())
                .isNotEmpty()
                .hasOnlyElementsOfType(PhoneWithExtension.class)
                .allSatisfy(phone -> {
                    PhoneWithExtension phoneExt = (PhoneWithExtension) phone;
                    assertThat(phoneExt.getExtension()).isNotBlank();
                });
    }

    @Test
    @DisplayName("Exclude fields from being populated using ignore()")
    void usingIgnore() {
        Phone phone = Instancio.of(Phone.class)
                .ignore(field(Phone::getNumber))
                .create();

        assertThat(phone.getNumber()).isNull();
    }

    @Test
    @DisplayName("withNullable() to allow null values to be generated for a given selector")
    void withNullable() {
        Set<String> results = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            Phone phone = Instancio.of(Phone.class)
                    .withNullable(field(Phone::getNumber)) // approx 1/6 probability of null
                    .create();

            results.add(phone.getNumber());
        }

        assertThat(results).containsNull();
    }

    @Test
    @DisplayName("withUnique() for generating unique values for a given selector")
    void withUnique() {
        List<Person> results = Instancio.ofList(Person.class)
                .size(100)
                .withUnique(field(Person::getFirstName))
                .create();

        assertThat(results).extracting(Person::getFirstName).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("assign() value conditionally")
    void assignConditional() {
        // Set Person.name based on the generated value of Person.gender
        Assignment assignment = Assign.given(field(Person::getGender), field(Person::getFirstName))
                .set(When.is(Gender.FEMALE), "Alice")
                .set(When.is(Gender.MALE), "Bob")
                .elseSet("Max");

        Person person = Instancio.of(Person.class)
                .assign(assignment)
                .create();

        if (person.getGender() == Gender.FEMALE) {
            assertThat(person.getFirstName()).isEqualTo("Alice");
        } else if (person.getGender() == Gender.MALE) {
            assertThat(person.getFirstName()).isEqualTo("Bob");
        } else {
            assertThat(person.getFirstName()).isEqualTo("Max");
        }
    }

    @Test
    @DisplayName("assign() value of one field to another field")
    void copyFieldValue() {
        Person person = Instancio.of(Person.class)
                .assign(Assign.valueOf(Person::getCreatedOn).to(Person::getLastModified))
                .create();

        assertThat(person.getLastModified()).isNotNull().isEqualTo(person.getCreatedOn());
    }

    @Test
    void cartesianProduct() {
        List<Person> persons = Instancio.ofCartesianProduct(Person.class)
                .with(field(Person::getGender), Gender.MALE, Gender.FEMALE)
                .with(field(Person::getAge), 30, 31, 32)
                .create();

        List<Pair<Gender, Integer>> expected = Arrays.asList(
                Pair.of(Gender.MALE, 30),
                Pair.of(Gender.MALE, 31),
                Pair.of(Gender.MALE, 32),
                Pair.of(Gender.FEMALE, 30),
                Pair.of(Gender.FEMALE, 31),
                Pair.of(Gender.FEMALE, 32));

        for (int i = 0; i < expected.size(); i++) {
            assertThat(persons.get(i).getGender()).isEqualTo(expected.get(i).getLeft());
            assertThat(persons.get(i).getAge()).isEqualTo(expected.get(i).getRight());
        }
    }

    @Test
    @DisplayName("filter() for filtering generated values using a predicate")
    void filter() {
        List<Person> results = Instancio.ofList(Person.class)
                .size(100)
                .filter(field(Person::getAge), (Integer age) -> age % 2 == 0)
                .create();

        assertThat(results)
                .hasSize(100)
                .allSatisfy(person -> assertThat(person.getAge()).isEven());
    }

    @Test
    @DisplayName("Generate a blank POJO with null fields")
    void setBlank() {
        Person personWithBlankAddress = Instancio.of(Person.class)
                .setBlank(field(Person::getAddress))
                .create();

        Address address = personWithBlankAddress.getAddress();

        assertThat(address).isNotNull();
        assertThat(address.getStreet()).isNull();
        assertThat(address.getCity()).isNull();
        assertThat(address.getCountry()).isNull();
        assertThat(address.getPhoneNumbers()).isEmpty(); // blank objects have empty Collections
    }
}
