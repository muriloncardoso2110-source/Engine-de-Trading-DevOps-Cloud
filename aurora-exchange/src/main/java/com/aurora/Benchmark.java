package com.aurora;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** In-process, open-loop generator. Reports percentile from end-to-end submission to result. */
public final class Benchmark {
 public static void main(String[] args)throws Exception{run(args);}
 public static void run(String[] args)throws Exception{
  int rate=args.length>1?Integer.parseInt(args[1]):100_000;
  int seconds=args.length>2?Integer.parseInt(args[2]):10;
  if(rate<=0||seconds<=0||seconds>120||((long)rate*seconds)>10_000_000)throw new IllegalArgumentException("rate/duration outside safety limits");
  int n=rate*seconds;long[] latency=new long[n];AtomicInteger done=new AtomicInteger(),errors=new AtomicInteger();
  try(Engine engine=new Engine()){
   System.out.println("Warmup...");for(int i=0;i<20_000;i++)engine.order(i%2+1,OrderBook.Side.BUY,99,1).join();
   long start=System.nanoTime();
   for(int i=0;i<n;i++){
    long target=start+(long)i*1_000_000_000L/rate;
    while(System.nanoTime()<target)Thread.onSpinWait();
    final int index=i;final long t=System.nanoTime();
    engine.order((i%100)+1,(i&1)==0?OrderBook.Side.BUY:OrderBook.Side.SELL,100,1)
       .whenComplete((res,err)->{latency[index]=System.nanoTime()-t;if(err!=null)errors.incrementAndGet();done.incrementAndGet();});
   }
   while(done.get()<n)Thread.sleep(1);
   long end=System.nanoTime();Arrays.sort(latency);
   double elapsed=(end-start)/1e9,achieved=n/elapsed;
   double p50=latency[(int)(n*.50)]/1e6,p95=latency[(int)(n*.95)]/1e6,p99=latency[(int)(n*.99)]/1e6,p999=latency[Math.min(n-1,(int)(n*.999))]/1e6;
   boolean pass=achieved>=rate*.95&&p99<10&&errors.get()==0;
   System.out.printf(Locale.US,"rate_target=%d total=%d achieved_rps=%.0f p50_ms=%.3f p95_ms=%.3f p99_ms=%.3f p999_ms=%.3f errors=%d PASS=%s%n",rate,n,achieved,p50,p95,p99,p999,errors.get(),pass);
   if(!pass)System.err.println("SLO FAILED: tune CPU, GC and concurrency; do not publish this as a passing result.");
  }
 }
}
