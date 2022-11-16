package de.focusshift.zeiterfassung.timeentry;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class FieldsNotAllEmptyValidator implements ConstraintValidator<FieldsNotAllEmpty, Object> {

    private String[] fields;

    @Override
    public void initialize(FieldsNotAllEmpty constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {

        final BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);

        final List<Tuple> nullValues = Stream.of(fields)
            .map(s -> new Tuple(s, beanWrapper.getPropertyValue(s)))
            .filter(tuple -> tuple.value instanceof String string ? !StringUtils.hasText(string) : Objects.isNull(tuple.value))
            .toList();

        final boolean valid = nullValues.size() != fields.length;

        if (!valid) {
            final String message = context.getDefaultConstraintMessageTemplate();
            context.disableDefaultConstraintViolation();

            for (Tuple tuple : nullValues) {
                context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(tuple.field)
                    .addConstraintViolation();
            }
        }

        return valid;
    }

    private record Tuple(String field, Object value) {
    }
}
