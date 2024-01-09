package org.example;

import org.instancio.Instancio;
import org.instancio.junit.InstancioExtension;
import org.instancio.junit.WithSettings;
import org.instancio.settings.AssignmentType;
import org.instancio.settings.Keys;
import org.instancio.settings.OnSetMethodNotFound;
import org.instancio.settings.OnSetMethodUnmatched;
import org.instancio.settings.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.instancio.Select.setter;

/**
 * Example of using method assignment to populate POJOs with dynamic attributes.
 */
@ExtendWith(InstancioExtension.class)
class Instancio9MethodAssignmentTest {

    /**
     * Populate objects via setters.
     * Ignore fields without a setter, e.g. {@link DynamicPerson#attributes}.
     * Invoke setter if it has no matching field, e.g. {@link DynamicPerson#setFavouriteFood(String)}.
     */
    @WithSettings
    private final Settings settings = Settings.create()
            .set(Keys.ASSIGNMENT_TYPE, AssignmentType.METHOD)
            .set(Keys.ON_SET_METHOD_NOT_FOUND, OnSetMethodNotFound.IGNORE)
            .set(Keys.ON_SET_METHOD_UNMATCHED, OnSetMethodUnmatched.INVOKE);

    private static class DynamicPerson {

        private final Map<String, String> attributes = new HashMap<>();

        private String name;

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        String getFavouriteFood() {
            return attributes.get("FAVOURITE_FOOD");
        }

        void setFavouriteFood(String favouriteFood) {
            attributes.put("FAVOURITE_FOOD", favouriteFood);
        }
    }

    @Test
    void createDynamicPerson() {
        DynamicPerson result = Instancio.create(DynamicPerson.class);

        assertThat(result.getName()).isNotBlank();
        assertThat(result.getFavouriteFood()).isNotBlank();
    }

    @Test
    void customiseDynamicPerson() {
        DynamicPerson result = Instancio.of(DynamicPerson.class)
                .set(field(DynamicPerson::getName), "Homer")
                .set(setter(DynamicPerson::setFavouriteFood), "donuts")
                .create();

        assertThat(result.getName()).isEqualTo("Homer");
        assertThat(result.getFavouriteFood()).isEqualTo("donuts");
    }
}
