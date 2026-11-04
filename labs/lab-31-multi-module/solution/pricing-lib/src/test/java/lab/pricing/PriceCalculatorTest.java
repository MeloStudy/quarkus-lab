package lab.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The library is plain Java: its rule is tested without Quarkus, in the library's own module. */
class PriceCalculatorTest {

    @Test
    void theTotalIncludesTheTax() {
        PriceCalculator calculator = new PriceCalculator();
        calculator.taxRate = 0.25;

        assertEquals(2500, calculator.total(2, 1000));
    }

    @Test
    void noQuantityCostsNothing() {
        PriceCalculator calculator = new PriceCalculator();
        calculator.taxRate = 0.20;

        assertEquals(0, calculator.total(0, 1000));
    }
}
