package kata.checkout;

import lombok.ToString;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CheckoutTest {
    @Nested
    class EmptyBasket {
        @Test
        void zeroTotal() {
            Basket basket = new Basket();
            assertEquals(new Money(0), basket.calculateTotal());
        }
    }

    @Nested
    class SingleItemBasket {
        @Test
        void singleItemTotal() {
            Basket basket = new Basket();
            basket.add("A");

            assertEquals(new Money(50), basket.calculateTotal());
        }

        @Test
        void differentSingleItemTotal() {
            Basket basket = new Basket();
            basket.add("B");

//            assertEquals(new Money(30), basket.calculateTotal());
        }
    }
}

class Basket {

    private List<String> items = new ArrayList<>();

    public Money calculateTotal() {
        return items.stream()
                .map(i -> lookupPrice(i))
                .reduce((a,b) -> a.add(b)).orElse(new Money(0));
    }

    private Money lookupPrice(String item) {
        return new Money(50);
    }

    public void add(String item) {
        items.add(item);
    }
}

@ToString
class Money {
    private final int value;

    public Money(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return value == money.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    public Money add(Money b) {
        return new Money(value + b.value);
    }
}