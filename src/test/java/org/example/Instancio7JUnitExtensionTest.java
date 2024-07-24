package org.example;

import org.example.person.Address;
import org.example.person.Person;
import org.instancio.Instancio;
import org.instancio.Result;
import org.instancio.junit.Given;
import org.instancio.junit.GivenProvider;
import org.instancio.junit.InstancioExtension;
import org.instancio.junit.InstancioSource;
import org.instancio.junit.Seed;
import org.instancio.junit.WithSettings;
import org.instancio.settings.Keys;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Instancio extension for JUnit 5 provides a few additional features:
 *
 * <ul>
 *   <li>{@code @Seed} annotation</li>
 *   <li>{@code @WithSettings} annotation for injecting {@link Settings}</li>
 *   <li>{@code @InstancioSource} for {@link ParameterizedTest} methods</li>
 *   <li>{@code @Given} annotation for injecting field and method arguments</li>
 *   <li>Reporting seed value when a test fails</li>
 * </ul>
 */
@ExtendWith(InstancioExtension.class)
class Instancio7JUnitExtensionTest {

    private static final int MIN_COLLECTION_SIZE = 5;

    @WithSettings
    private static final Settings settings = Settings.create()
            // generated strings will be prefixed with field names
            .set(Keys.STRING_FIELD_PREFIX_ENABLED, true)
            .set(Keys.COLLECTION_MIN_SIZE, MIN_COLLECTION_SIZE)
            .lock();

    @Given
    private Address address;

    @Test
    @DisplayName("@Given for injecting fields")
    void injectFieldsUsingGiven() {
        assertThat(address).hasNoNullFieldsOrProperties();
    }

    @Test
    @DisplayName("@Given for injecting method parameters (supports @Test, @RepeatedTest, @ParameterizedTest)")
    void injectParametersUsingGiven(@Given Person person, @Given UUID uuid) {
        assertThat(person).hasNoNullFieldsOrProperties();
        assertThat(uuid).isNotNull();
    }

    @Test
    @DisplayName("Using a custom annotation; supports injecting a single object, Supplier, or Stream")
    void givenWithCustomProvider(
            @ProductCode String singleProductCode,
            @ProductCode Supplier<String> supplierOfProductCode,
            @ProductCode Stream<String> streamOfProductCodes) {

        final String expectedPattern = "^[A-Z]{3}-[0-9]{5}$";

        assertThat(singleProductCode).matches(expectedPattern);

        assertThat(supplierOfProductCode.get()).matches(expectedPattern);

        // The returned Stream is infinite (limit() must be called to prevent an infinite loop)
        final Stream<String> productCodes = streamOfProductCodes.limit(100);

        assertThat(productCodes)
                .hasSize(100)
                .allSatisfy(productCode -> assertThat(productCode).matches(expectedPattern));
    }

    @Given(ProductCode.ProductCodeProvider.class)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ProductCode {

        class ProductCodeProvider implements GivenProvider {
            @Override
            public Object provide(ElementContext context) {
                return Instancio.gen().text().pattern("#C#C#C-#d#d#d#d#d").get(); // e.g. "ABC-12345"
            }
        }
    }

    @Test
    @DisplayName("Should use Settings overrides for all strings and collections")
    void withCustomSettings() {
        Person person = Instancio.create(Person.class);

        assertThat(person.getFirstName()).startsWith("firstName_");
        assertThat(person.getAddress().getCity()).startsWith("city_");
        assertThat(person.getAddress().getPhoneNumbers())
                .hasSizeGreaterThanOrEqualTo(MIN_COLLECTION_SIZE);
    }

    /**
     * Runs the test 10 times with different arguments.
     */
    @InstancioSource(samples = 10)
    @ParameterizedTest
    @DisplayName("Using Instancio to provide any number of parameterized test arguments")
    void parameterizedExample(UUID uuid, Address address) {
        assertThat(uuid).isNotNull();
        assertThat(address).isNotNull();
    }

    /**
     * When a test that uses {@link InstancioExtension} fails,
     * it reports the seed value that was used to generate the data.
     *
     * <p>Using the {@code @Seed} annotation allows reproducing
     * the data in case of test failure.
     */
    @Seed(12345)
    @Test
    @DisplayName("Reproducing data using the @Seed annotation")
    void shouldGenerateDataBasedOnGivenSeed() {
        Result<Address> result = Instancio.of(Address.class).asResult();

        assertThat(result.get()).isNotNull();
        assertThat(result.getSeed()).isEqualTo(12345);
    }
}
