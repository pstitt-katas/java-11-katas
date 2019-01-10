package kata.list;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SinglyLinkedListTest {
    @Nested
    class Building {
        @Test
        void oneElement() {
            SinglyLinkedList list = ListBuilder.buildFrom(1);
            assertEquals(1, list.size());
            assertEquals("(1 )", list.toString());
        }

        @Test
        void multipleElements() {
            SinglyLinkedList list = ListBuilder.buildFrom(1, 2, 3, 4, 5, 6, 7);
            assertEquals(7, list.size());
            assertEquals("(1 2 3 4 5 6 7 )", list.toString());
        }
    }

    @Nested
    class Retrieval {
        SinglyLinkedList list7 = ListBuilder.buildFrom(1, 2, 3, 4, 5, 6, 7);

        @Test
        void middle() {
            assertEquals(6, list7.elementAt(5).getHeadValue());
        }

        @Test
        void head() {
            assertEquals(1, list7.elementAt(0).getHeadValue());
        }

        @Test
        void tail() {
            assertEquals(7, list7.elementAt(6).getHeadValue());
        }
    }

    @Nested
    class Insertion {
        SinglyLinkedList list7 = ListBuilder.buildFrom(1, 2, 3, 4, 5, 6, 7);

        @Test
        void middle() {
            SinglyLinkedList list8 = list7.insertAfter(0, 99);
            assertNotEquals(list7, list8);
            assertEquals(8, list8.size());
            assertEquals("(1 99 2 3 4 5 6 7 )", list8.toString());
        }
    }
}

class SinglyLinkedList {
    private final int head;
    private final SinglyLinkedList tail;

    public int getHeadValue() {
        return head;
    }

    public int size() {
        return 1 + (tail == null ? 0 : tail.size());
    }

    public SinglyLinkedList elementAt(int i) {
        SinglyLinkedList current = this;
        for (int j=1; j<=i; j++) {
            current = current.tail;
        }
        return current;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int element : toArray()) {
            builder.append(element);
            builder.append(" ");
        }
        builder.append(")");
        return builder.toString();
    }

    public SinglyLinkedList insertAfter(int i, int value) {
        int[] newElements = new int[size()+1];

        copySourceArrayIntoTargetArrayAtIndex(subArray(0, i), newElements, 0);
        newElements[i+1] = value;
        copySourceArrayIntoTargetArrayAtIndex(subArray(i+1, size()-1), newElements, i+2);

        return ListBuilder.buildFrom(newElements);
    }

    private static void copySourceArrayIntoTargetArrayAtIndex(int[] source, int[] target, int targetIndex) {
        for (int i=0; i<source.length; i++) {
            target[targetIndex+i] = source[i];
        }
    }

    protected SinglyLinkedList(int head, SinglyLinkedList tail) {
        this.head = head;
        this.tail = tail;
    }

    protected int[] toArray() {
        return subArray(0, size()-1);
    }

    protected int[] subArray(int startIndex, int endIndex) {
        int[] array = new int[endIndex-startIndex+1];
        SinglyLinkedList next = elementAt(startIndex);
        for (int i=startIndex; i<=endIndex; i++) {
            array[i-startIndex] = next.head;
            next = next.tail;
        }
        return array;
    }
}

class ListBuilder {
    public static SinglyLinkedList buildFrom(int... elements) {
        SinglyLinkedList listBeingBuilt = null;
        for (int i = elements.length-1; i>=0; i--) {
            listBeingBuilt = new SinglyLinkedList(elements[i], listBeingBuilt);
        }
        return listBeingBuilt;
    }
}
