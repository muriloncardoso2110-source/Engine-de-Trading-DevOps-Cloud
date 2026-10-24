package com.aurora;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

/** Preallocated ring slots; all order-book state is exclusively owned by the consumer thread. */
public final class Engine implements AutoCloseable {
  static final class Event {long owner,price,qty,orderId,start;OrderBook.Side side;int type;CompletableFuture<Object> response;void clear(){response=null;side=null;}}
  private final OrderBook book=new OrderBook();
  private final Disruptor<Event> disruptor;
  private final RingBuffer<Event> ring;
  private final AtomicLong processed=new AtomicLong(), rejected=new AtomicLong(), latencyTotalNs=new AtomicLong(), latencyMaxNs=new AtomicLong();
  public Engine(){
    ThreadFactory factory=r->{Thread t=new Thread(r,"aurora-matching-core");t.setDaemon(true);return t;};
    disruptor=new Disruptor<>(Event::new,65536,factory,ProducerType.MULTI,new BlockingWaitStrategy());
    disruptor.handleEventsWith((event,seq,end)->{try{
      Object result=switch(event.type){
        case 1 -> book.place(event.owner,event.side,event.price,event.qty);
        case 2 -> book.cancel(event.owner,event.orderId);
        case 3 -> book.account(event.owner);
        case 4 -> book.book();
        case 5 -> book.recentTrades();
        default -> "invalid";
      };
      if(event.type==1){processed.incrementAndGet();if(((OrderBook.Result)result).status().equals("REJECTED"))rejected.incrementAndGet();}
      if(event.response!=null)event.response.complete(result);
      if(event.start!=0){long ns=System.nanoTime()-event.start;latencyTotalNs.addAndGet(ns);latencyMaxNs.accumulateAndGet(ns,Math::max);}
    }catch(Throwable error){if(event.response!=null)event.response.completeExceptionally(error);}finally{event.clear();}});
    ring=disruptor.start();
  }
  private CompletableFuture<Object> send(int type,long owner,OrderBook.Side side,long price,long qty,long orderId){
    CompletableFuture<Object> future=new CompletableFuture<>();
    long seq=ring.next();try{Event e=ring.get(seq);e.type=type;e.owner=owner;e.side=side;e.price=price;e.qty=qty;e.orderId=orderId;e.start=System.nanoTime();e.response=future;}finally{ring.publish(seq);}return future;
  }
  public CompletableFuture<Object> order(long owner,OrderBook.Side side,long price,long qty){return send(1,owner,side,price,qty,0);}
  public CompletableFuture<Object> cancel(long owner,long orderId){return send(2,owner,null,0,0,orderId);}
  public CompletableFuture<Object> account(long owner){return send(3,owner,null,0,0,0);}
  public CompletableFuture<Object> book(){return send(4,0,null,0,0,0);}
  public CompletableFuture<Object> trades(){return send(5,0,null,0,0,0);}
  public String metrics(){long p=processed.get();return "aurora_orders_processed_total "+p+"\naurora_orders_rejected_total "+rejected.get()+"\naurora_matching_latency_average_nanoseconds "+(p==0?0:latencyTotalNs.get()/p)+"\naurora_matching_latency_max_nanoseconds "+latencyMaxNs.get()+"\n";}
  public long processed(){return processed.get();}
  @Override public void close(){disruptor.shutdown(10,TimeUnit.SECONDS);}
}
