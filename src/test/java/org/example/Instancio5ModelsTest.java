package org.example;

import org.example.person.Address;
import org.example.person.Gender;
import org.example.person.Person;
import org.example.person.Phone;
import org.instancio.Assign;
import org.instancio.Instancio;
import org.instancio.Model;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.all;
import static org.instancio.Select.field;

/**
 * Examples of using {@link org.instancio.Model}.
 * A model is a template for creating objects.
 */
class Instancio5ModelsTest {

    /*
     Define a model and create objects from it based on the template.
     Tests create objects based on the model and apply customisations on top of it if needed
    */
    private final Model<Person> simpsonsModel = Instancio.of(Person.class)
            .set(field(Person::getLastName), "Simpson")
            // Create 'fullName' from first and last names
            .assign(Assign.valueOf(all(Person.class))
                    .to(field(Person::getFullName))
                    .as((Person p) -> p.getFirstName() + " " + p.getLastName()))
            .set(field(Address::getCity), "Springfield")
            .set(field(Address::getCountry), "US")
            .set(field(Phone::getCountryCode), "+1")
            .generate(field(Phone::getNumber), gen -> gen.text().pattern("#d#d#d-#d#d-#d#d"))
            .toModel();

    @Test
    void createSimpsonsFromModel() {
        Person homer = Instancio.of(simpsonsModel)
                .set(field(Person::getFirstName), "Homer")
                .set(all(Gender.class), Gender.MALE)
                .create();

        assertThat(homer.getFirstName()).isEqualTo("Homer");
        assertThat(homer.getLastName()).isEqualTo("Simpson");
        assertThat(homer.getFullName()).isEqualTo("Homer Simpson");
        assertThat(homer.getGender()).isEqualTo(Gender.MALE);

        Address address = homer.getAddress();
        assertThat(address.getCity()).isEqualTo("Springfield");
        assertThat(address.getCountry()).isEqualTo("US");
        assertThat(address.getPhoneNumbers())
                .isNotEmpty()
                .allSatisfy(phone -> assertThat(phone.getCountryCode()).isEqualTo("+1"));
    }

    @Test
    @DisplayName("Models can be created from other Models")
    void createModelFromModel() {
        Model<Person> simpsonsKid = Instancio.of(simpsonsModel)
                .generate(field("age"), gen -> gen.ints().range(5, 10))
                .toModel();

        Person bart = Instancio.of(simpsonsKid)
                .set(field(Person::getFirstName), "Bart")
                .set(all(Gender.class), Gender.MALE)
                .create();

        assertThat(bart.getFirstName()).isEqualTo("Bart");
        assertThat(bart.getFullName()).isEqualTo("Bart Simpson");
        assertThat(bart.getGender()).isEqualTo(Gender.MALE);
        assertThat(bart.getAge()).isBetween(5, 10);

        Address address = bart.getAddress();
        assertThat(address.getCity()).isEqualTo("Springfield");
        assertThat(address.getCountry()).isEqualTo("US");
        assertThat(address.getPhoneNumbers())
                .isNotEmpty()
                .allSatisfy(phone -> assertThat(phone.getCountryCode()).isEqualTo("+1"));
    }

    @Test
    void createCollectionFromModel() {
        final int numberOfFamilyMembers = 5;

        List<Person> simpsons = Instancio.ofList(simpsonsModel)
                .size(numberOfFamilyMembers)
                .create();

        assertThat(simpsons)
                .as("All family members live at the same address")
                .hasSize(numberOfFamilyMembers)
                .allSatisfy(simpson -> {
                    Address address = simpson.getAddress();
                    assertThat(address.getCity()).isEqualTo("Springfield");
                    assertThat(address.getCountry()).isEqualTo("US");
                    assertThat(address.getPhoneNumbers())
                            .isNotEmpty()
                            .allSatisfy(phone -> assertThat(phone.getCountryCode()).isEqualTo("+1"));
                });
    }

}
