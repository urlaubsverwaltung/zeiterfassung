package de.focusshift.zeiterfassung.web;

import org.springframework.format.Formatter;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Allows text values to define a double separated with <em>comma</em> or <em>dot</em>, doesn't matter.
 * Default would be the current {@linkplain Locale} format.
 */
@Component
public class DoubleFormatter implements Formatter<Double> {

    @Override
    public Double parse(String text, Locale locale) throws ParseException {

        final DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance(locale);

        if (text.contains(".")) {
            decimalFormatSymbols.setDecimalSeparator('.');
        } else if (text.contains(",")) {
            decimalFormatSymbols.setDecimalSeparator(',');
        }

        final DecimalFormat decimalFormat = new DecimalFormat("", decimalFormatSymbols);
        decimalFormat.setGroupingUsed(false);

        return decimalFormat.parse(text).doubleValue();
    }

    @Override
    public String print(Double object, Locale locale) {

        final NumberFormat numberFormat = NumberFormat.getInstance(locale);
        numberFormat.setGroupingUsed(false);

        return numberFormat.format(object);
    }
}
