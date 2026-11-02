package lab.melostudy;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/**
 * Teaches Jackson to write and read {@link Money} as {@code "120.50 EUR"}. It is registered in the application's
 * ObjectMapper by a CDI bean, so {@code Money} itself stays free of Jackson annotations (it could live in a
 * shared library that we cannot annotate).
 */
@Singleton
public class MoneyJson implements ObjectMapperCustomizer {

    private static final Pattern FORMAT = Pattern.compile("(\\d{1,7}(?:\\.\\d{1,2})?) ([A-Z]{3})");

    @Override
    public void customize(com.fasterxml.jackson.databind.ObjectMapper mapper) {
        SimpleModule module = new SimpleModule("money");
        module.addSerializer(Money.class, new Writer());
        module.addDeserializer(Money.class, new Reader());
        mapper.registerModule(module);
    }

    static class Writer extends StdSerializer<Money> {
        Writer() {
            super(Money.class);
        }

        @Override
        public void serialize(Money money, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(money.amount().toPlainString() + " " + money.currency());
        }
    }

    static class Reader extends StdDeserializer<Money> {
        Reader() {
            super(Money.class);
        }

        @Override
        public Money deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String text = parser.getValueAsString();
            Matcher matcher = text == null ? null : FORMAT.matcher(text);
            if (matcher == null || !matcher.matches()) {
                // reports a client error (400), with the position in the document, instead of failing with a 500
                return (Money) context.handleWeirdStringValue(Money.class, text, "expected \"<amount> <CUR>\", e.g. \"120.50 EUR\"");
            }
            return new Money(new BigDecimal(matcher.group(1)), matcher.group(2));
        }
    }
}
