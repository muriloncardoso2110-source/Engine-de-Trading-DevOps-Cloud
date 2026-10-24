package com.aurora;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderBookAdditionalTest {
    @Test void honorsPricePriority() {
        OrderBook book = new OrderBook();
        book.place(2, OrderBook.Side.SELL, 105, 3);
        book.place(3, OrderBook.Side.SELL, 100, 3);
        book.place(1, OrderBook.Side.BUY, 110, 3);
        var trade = book.recentTrades().getFirst();
        assertEquals(100, trade.price());
        assertEquals(3, trade.quantity());
        assertEquals(1, book.totalTrades());
    }

    @Test void preservesFifoAtSamePrice() {
        OrderBook book = new OrderBook();
        var first = book.place(2, OrderBook.Side.SELL, 100, 1);
        book.place(3, OrderBook.Side.SELL, 100, 1);
        book.place(1, OrderBook.Side.BUY, 100, 1);
        assertEquals(first.id(), book.recentTrades().getFirst().maker());
    }

    @Test void rejectsUnknownAccountWithoutChangingBook() {
        OrderBook book = new OrderBook();
        assertEquals("REJECTED", book.place(1000, OrderBook.Side.BUY, 100, 1).status());
        assertEquals(0, book.totalTrades());
        assertTrue(book.book().contains("\"bidLevels\":0"));
    }

    @Test void cancelCannotTouchOtherAccountsOrder() {
        OrderBook book = new OrderBook();
        var order = book.place(2, OrderBook.Side.SELL, 100, 10);
        assertFalse(book.cancel(1, order.id()));
        assertEquals(10, book.account(2).reservedAsset());
        assertTrue(book.cancel(2, order.id()));
        assertEquals(0, book.account(2).reservedAsset());
    }
}
