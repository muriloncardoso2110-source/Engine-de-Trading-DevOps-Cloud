package com.aurora;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class OrderBookTest {
 @Test void matchesFifoAndSettles(){OrderBook b=new OrderBook();long a=b.account(1).cash(),s=b.account(2).cash();
  var sell=b.place(2,OrderBook.Side.SELL,100,5);assertEquals("OPEN",sell.status());
  var buy=b.place(1,OrderBook.Side.BUY,120,5);assertEquals("FILLED",buy.status());assertEquals(5,buy.filled());
  assertEquals(a-500,b.account(1).cash());assertEquals(s+500,b.account(2).cash());assertEquals(1,b.totalTrades());
 }
 @Test void rejectsUnfundedAndCancels(){OrderBook b=new OrderBook();assertEquals("REJECTED",b.place(1,OrderBook.Side.BUY,1_000_000_000L,1_000_000L).status());
  var o=b.place(1,OrderBook.Side.BUY,100,4);assertTrue(b.cancel(1,o.id()));assertEquals(0,b.account(1).reservedCash());
 }
 @Test void noSelfTrade(){OrderBook b=new OrderBook();b.place(1,OrderBook.Side.SELL,100,1);b.place(1,OrderBook.Side.BUY,100,1);assertEquals(0,b.totalTrades());}
}
