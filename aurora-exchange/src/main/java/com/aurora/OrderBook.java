package com.aurora;

import java.util.*;

/** Single-writer price-time priority book, with pre-funded cash and inventory. All amounts are integers. */
public final class OrderBook {
  public enum Side { BUY, SELL }
  public record Result(long id, long filled, long resting, long trades, String status, String reason) {}
  public record Account(long cash, long reservedCash, long asset, long reservedAsset) {}
  public record Trade(long id, long maker, long taker, long price, long quantity) {}
  static final class Balance {long cash, reservedCash, asset, reservedAsset; Balance(long c,long a){cash=c;asset=a;}}
  static final class Order {long id, owner, price, remaining; Side side; Order(long id,long owner,long price,long qty,Side side){this.id=id;this.owner=owner;this.price=price;remaining=qty;this.side=side;}}
  private final NavigableMap<Long,ArrayDeque<Order>> bids=new TreeMap<>(Comparator.reverseOrder());
  private final NavigableMap<Long,ArrayDeque<Order>> asks=new TreeMap<>();
  private final HashMap<Long,Balance> accounts=new HashMap<>();
  private final ArrayDeque<Trade> recentTrades=new ArrayDeque<>();
  private long tradeId=0, totalTrades=0, nextOrderId=0;
  public OrderBook(){for(long i=1;i<=100;i++) accounts.put(i,new Balance(100_000_000_000L,10_000_000L));}
  public Account account(long id){Balance b=accounts.get(id);return b==null?null:new Account(b.cash,b.reservedCash,b.asset,b.reservedAsset);}
  public long totalTrades(){return totalTrades;}
  public List<Trade> recentTrades(){return List.copyOf(recentTrades);}
  public String book(){return "{\"bestBid\":"+(bids.isEmpty()?"null":bids.firstKey())+",\"bestAsk\":"+(asks.isEmpty()?"null":asks.firstKey())+",\"bidLevels\":"+bids.size()+",\"askLevels\":"+asks.size()+",\"trades\":"+totalTrades+"}";}
  public Result place(long owner,Side side,long price,long qty) {
    long id=++nextOrderId;
    if(price<=0||qty<=0||qty>1_000_000L||price>1_000_000_000L) return new Result(id,0,0,0,"REJECTED","invalid price or quantity");
    Balance taker=accounts.get(owner);
    if(taker==null) return new Result(id,0,0,0,"REJECTED","unknown account: 1..100");
    long cost=Math.multiplyExact(price,qty);
    if(side==Side.BUY){if(taker.cash<cost) return new Result(id,0,0,0,"REJECTED","insufficient cash");taker.cash-=cost;taker.reservedCash+=cost;}
    else {if(taker.asset<qty)return new Result(id,0,0,0,"REJECTED","insufficient inventory");taker.asset-=qty;taker.reservedAsset+=qty;}
    Order incoming=new Order(id,owner,price,qty,side);
    NavigableMap<Long,ArrayDeque<Order>> opposite=side==Side.BUY?asks:bids;
    long matched=0, count=0;
    while(incoming.remaining>0&&!opposite.isEmpty()) {
      long best=opposite.firstKey();
      if(side==Side.BUY?best>price:best<price)break;
      ArrayDeque<Order> q=opposite.firstEntry().getValue();Order maker=q.peek();
      if(maker.owner==owner){ // cancel resting maker to prevent wash trades; release its reserves
        release(maker);q.remove();if(q.isEmpty())opposite.pollFirstEntry();continue;
      }
      long units=Math.min(incoming.remaining,maker.remaining), amount=Math.multiplyExact(best,units);
      Balance seller=accounts.get(side==Side.BUY?maker.owner:owner);
      Balance buyer=accounts.get(side==Side.BUY?owner:maker.owner);
      seller.reservedAsset-=units;seller.cash+=amount;
      long buyerLimit=side==Side.BUY?price:maker.price;
      buyer.reservedCash-=Math.multiplyExact(buyerLimit,units);
      buyer.cash+=Math.multiplyExact(buyerLimit-best,units);
      buyer.asset+=units;
      incoming.remaining-=units;maker.remaining-=units;matched+=units;count++;totalTrades++;
      recentTrades.addLast(new Trade(++tradeId,maker.id,id,best,units));if(recentTrades.size()>50)recentTrades.removeFirst();
      if(maker.remaining==0){q.remove();if(q.isEmpty())opposite.pollFirstEntry();}
    }
    if(incoming.remaining>0)(side==Side.BUY?bids:asks).computeIfAbsent(price,k->new ArrayDeque<>()).addLast(incoming);
    return new Result(id,matched,incoming.remaining,count,incoming.remaining==0?"FILLED":matched>0?"PARTIAL":"OPEN","");
  }
  private void release(Order o){Balance b=accounts.get(o.owner);if(o.side==Side.BUY){long amount=Math.multiplyExact(o.price,o.remaining);b.reservedCash-=amount;b.cash+=amount;}else{b.reservedAsset-=o.remaining;b.asset+=o.remaining;}}
  public boolean cancel(long owner,long orderId){for(var map:List.of(bids,asks))for(var it=map.entrySet().iterator();it.hasNext();){var e=it.next();for(var oi=e.getValue().iterator();oi.hasNext();){Order o=oi.next();if(o.id==orderId&&o.owner==owner){release(o);oi.remove();if(e.getValue().isEmpty())it.remove();return true;}}}return false;}
}
