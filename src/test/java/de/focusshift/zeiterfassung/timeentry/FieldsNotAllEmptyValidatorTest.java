package de.focusshift.zeiterfassung.timeentry;

import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldsNotAllEmptyValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilderCustomizableContext;

    @Test
    void allFieldsAreNotEmpty() {

        final LocalTime start = LocalTime.of(10, 10, 10);
        final LocalTime end = LocalTime.of(10, 20, 10);
        final SomeEntryDto entry = new SomeEntryDto(start, end, "11:00");

        final FieldsNotAllEmptyValidator validator = new FieldsNotAllEmptyValidator();

        validator.initialize(createAnnotation("start", "end", "duration"));
        final boolean valid = validator.isValid(entry, constraintValidatorContext);
        assertThat(valid).isTrue();

        verifyNoInteractions(constraintValidatorContext);
    }

    @Test
    void allFieldsAreEmpty() {

        final String messageTemplate = "message";
        when(constraintValidatorContext.getDefaultConstraintMessageTemplate()).thenReturn(messageTemplate);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(messageTemplate)).thenReturn(constraintViolationBuilder);
        when(constraintViolationBuilder.addPropertyNode("start")).thenReturn(nodeBuilderCustomizableContext);
        when(constraintViolationBuilder.addPropertyNode("end")).thenReturn(nodeBuilderCustomizableContext);
        when(constraintViolationBuilder.addPropertyNode("duration")).thenReturn(nodeBuilderCustomizableContext);

        final SomeEntryDto entry = new SomeEntryDto(null, null, null);

        final FieldsNotAllEmptyValidator validator = new FieldsNotAllEmptyValidator();

        validator.initialize(createAnnotation("start", "end", "duration"));
        final boolean valid = validator.isValid(entry, constraintValidatorContext);
        assertThat(valid).isFalse();

        verify(constraintValidatorContext).disableDefaultConstraintViolation();
        verify(constraintViolationBuilder).addPropertyNode("start");
        verify(constraintViolationBuilder).addPropertyNode("end");
        verify(constraintViolationBuilder).addPropertyNode("duration");
        verify(nodeBuilderCustomizableContext, times(3)).addConstraintViolation();
    }

    private FieldsNotAllEmpty createAnnotation(String... values) {
        final AnnotationDescriptor descriptor = new AnnotationDescriptor(FieldsNotAllEmpty.class);
        descriptor.setValue("fields", values);

        return AnnotationFactory.create(descriptor);
    }

    private static class SomeEntryDto {

        private final LocalTime start;
        private final LocalTime end;
        private final String duration;

        SomeEntryDto(LocalTime start, LocalTime end, String duration) {
            this.start = start;
            this.end = end;
            this.duration = duration;
        }

        public LocalTime getStart() {
            return start;
        }

        public LocalTime getEnd() {
            return end;
        }

        public String getDuration() {
            return duration;
        }
    }
}
