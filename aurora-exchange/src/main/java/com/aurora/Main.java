package com.aurora;
import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class Main {
  static final int PORT=Integer.parseInt(System.getenv().getOrDefault("PORT","8080"));
  public static void main(String[] args)throws Exception{
    if(args.length>0&&args[0].equals("benchmark")){Benchmark.run(args);return;}
    Engine engine=new Engine();HttpServer server=HttpServer.create(new InetSocketAddress("0.0.0.0",PORT),256);
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.createContext("/",exchange->{try{route(exchange,engine);}catch(Exception e){send(exchange,500,"{\"error\":\"internal error\"}");}});
    Runtime.getRuntime().addShutdownHook(new Thread(()->{server.stop(1);engine.close();}));
    server.start();System.out.println("Aurora Exchange HTTP listening on :"+PORT);
  }
  static void route(HttpExchange x,Engine e)throws Exception{
    String path=x.getRequestURI().getPath(),method=x.getRequestMethod();
    if(path.equals("/health")){send(x,200,"{\"status\":\"UP\"}");return;}
    if(path.equals("/metrics")){byte[] data=e.metrics().getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","text/plain; version=0.0.4");x.sendResponseHeaders(200,data.length);try(var out=x.getResponseBody()){out.write(data);}return;}
    if(path.equals("/")){send(x,200,"{\"service\":\"Aurora Exchange\",\"routes\":[\"GET /health\",\"GET /metrics\",\"GET /book\",\"GET /accounts?id=1\",\"GET /trades\",\"POST /orders\",\"POST /cancel\"]}");return;}
    Map<String,String> q=parse(x.getRequestURI().getRawQuery());
    if(method.equals("POST")){if(x.getRequestBody().available()>8192){send(x,413,"{\"error\":\"payload too large\"}");return;}String body=new String(x.getRequestBody().readNBytes(8193),StandardCharsets.UTF_8);if(body.length()>8192){send(x,413,"{\"error\":\"payload too large\"}");return;}q.putAll(parse(body));}
    try{
      switch(path){
        case "/orders" -> {if(!method.equals("POST")){send(x,405,"{\"error\":\"POST required\"}");return;}
          long owner=Long.parseLong(q.getOrDefault("account","1")),price=Long.parseLong(q.getOrDefault("price","0")),qty=Long.parseLong(q.getOrDefault("quantity","0"));
          OrderBook.Side side=OrderBook.Side.valueOf(q.getOrDefault("side","").toUpperCase(Locale.ROOT));
          OrderBook.Result r=(OrderBook.Result)e.order(owner,side,price,qty).get(5,TimeUnit.SECONDS);
          send(x,r.status().equals("REJECTED")?422:200,"{\"orderId\":"+r.id()+",\"filled\":"+r.filled()+",\"resting\":"+r.resting()+",\"trades\":"+r.trades()+",\"status\":\""+r.status()+"\",\"reason\":\""+r.reason()+"\"}");
        }
        case "/cancel" -> {if(!method.equals("POST")){send(x,405,"{\"error\":\"POST required\"}");return;}boolean ok=(Boolean)e.cancel(Long.parseLong(q.get("account")),Long.parseLong(q.get("orderId"))).get(5,TimeUnit.SECONDS);send(x,ok?200:404,"{\"cancelled\":"+ok+"}");}
        case "/accounts" -> {OrderBook.Account a=(OrderBook.Account)e.account(Long.parseLong(q.getOrDefault("id","1"))).get(5,TimeUnit.SECONDS);send(x,a==null?404:200,a==null?"{\"error\":\"unknown account\"}":"{\"cash\":"+a.cash()+",\"reservedCash\":"+a.reservedCash()+",\"asset\":"+a.asset()+",\"reservedAsset\":"+a.reservedAsset()+"}");}
        case "/book" -> send(x,200,(String)e.book().get(5,TimeUnit.SECONDS));
        case "/trades" -> {var trades=(List<?>)e.trades().get(5,TimeUnit.SECONDS);StringJoiner json=new StringJoiner(",","[","]");for(Object item:trades){OrderBook.Trade t=(OrderBook.Trade)item;json.add("{\"tradeId\":"+t.id()+",\"maker\":"+t.maker()+",\"taker\":"+t.taker()+",\"price\":"+t.price()+",\"quantity\":"+t.quantity()+"}");}send(x,200,json.toString());}
        default -> send(x,404,"{\"error\":\"not found\"}");
      }
    }catch(IllegalArgumentException|NullPointerException ex){send(x,400,"{\"error\":\"invalid input\"}");}
  }
  static Map<String,String> parse(String s){Map<String,String> map=new HashMap<>();if(s==null||s.isBlank())return map;for(String pair:s.split("&")){String[] bits=pair.split("=",2);map.put(URLDecoder.decode(bits[0],StandardCharsets.UTF_8),URLDecoder.decode(bits.length>1?bits[1]:"",StandardCharsets.UTF_8));}return map;}
  static void send(HttpExchange x,int code,String s)throws java.io.IOException{byte[] data=s.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");x.sendResponseHeaders(code,data.length);try(var out=x.getResponseBody()){out.write(data);}}
}
