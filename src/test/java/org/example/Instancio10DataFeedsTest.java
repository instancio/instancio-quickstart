package org.example;

import org.example.person.Gender;
import org.example.person.Person;
import org.instancio.Instancio;
import org.instancio.feed.Feed;
import org.instancio.feed.FeedSpec;
import org.instancio.feed.FunctionProvider;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.all;

/**
 * Example of using data feeds
 */
@ExtendWith(InstancioExtension.class)
class Instancio10DataFeedsTest {

    @Test
    @DisplayName("Retrieve certain data properties from the feed")
    void createFeedFromFile() {
        Feed personFeed = Instancio.ofFeed(Feed.class)
                .withDataSource(source -> source.ofResource("persons.csv"))
                .create();

        final FeedSpec<String> firstName = personFeed.stringSpec("firstName");
        final FeedSpec<Integer> age = personFeed.intSpec("age");

        for (int i = 0; i < 3; i++) {
            System.out.println(firstName.get() + " " + age.get());
        }
    }

    @Test
    @DisplayName("Mapping data feed to objects using applyFeed()")
    void applyFeed() {
        Feed personFeed = Instancio.ofFeed(Feed.class)
                .withDataSource(source -> source.ofResource("persons.csv"))
                .create();

        List<Person> persons = Instancio.ofList(Person.class)
                .size(3)
                .applyFeed(all(Person.class), personFeed)
                .create();

        assertThat(persons).extracting(Person::getFirstName).contains("John", "Alice", "Bobby");
        assertThat(persons).extracting(Person::getGender).contains(Gender.MALE, Gender.FEMALE, Gender.MALE);
    }

    /**
     * A feed can also be defined as an interface, which provides support
     * for additional features via the following annotations:
     *
     * <ul>
     *   <li>{@link org.instancio.feed.FeedSpecAnnotations.DataSpec}</li>
     *   <li>{@link org.instancio.feed.FeedSpecAnnotations.FunctionSpec}</li>
     *   <li>{@link org.instancio.feed.FeedSpecAnnotations.GeneratedSpec}</li>
     *   <li>{@link org.instancio.feed.FeedSpecAnnotations.NullableSpec}</li>
     *   <li>{@link org.instancio.feed.FeedSpecAnnotations.TemplateSpec}</li>
     *   <li>{@link org.instancio.feed.FeedSpecAnnotations.WithPostProcessor}</li>
     *   <li>{@link org.instancio.feed.FeedSpecAnnotations.WithStringMapper}</li>
     * </ul>
     */
    @Feed.Source(resource = "persons.csv")
    interface PersonFeed extends Feed {

        @TemplateSpec("${firstName} ${lastName}")
        FeedSpec<String> fullName();

        FeedSpec<Integer> age();

        @FunctionSpec(params = "age", provider = IsAdultProvider.class)
        FeedSpec<Boolean> isAdult();

        class IsAdultProvider implements FunctionProvider {
            boolean calculateDob(int age) {
                return age >= 18;
            }
        }
    }

    @Test
    @DisplayName("Using custom feed interface")
    void customFeedInterface() {
        List<Person> persons = Instancio.ofList(Person.class)
                .size(3)
                .applyFeed(all(Person.class), feed -> feed.of(PersonFeed.class))
                .create();

        assertThat(persons).extracting(Person::getFullName).contains("John Doe", "Alice Smith", "Bobby Brown");
        assertThat(persons).extracting(Person::getAge).contains(21, 34, 15);
        assertThat(persons).extracting(Person::isAdult).contains(true, true, false);
    }
}
