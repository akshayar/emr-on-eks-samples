package com.aksh.rand.mysql;

import com.github.javafaker.Faker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@SpringBootApplication
public class Main implements CommandLineRunner {
    private static Logger log= LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Autowired
    private TradeInfoMapper tradeInfoMapper;

    Faker faker=new Faker();

    private BlockingQueue<TradeInfo> prodIdQueue=new LinkedBlockingDeque<>();

    @Override
    public void run(String... args) throws Exception {
        Arrays.asList(args).forEach(log::warn);
        log.warn("Proerties:"+System.getProperties());

        Random random=new Random();
        log.warn("Delay:"+System.getProperty("delay"));
        String delay=Optional.ofNullable(System.getProperty("delay"))
                .filter(s->!StringUtils.isEmpty(s))
                .orElse("10");

        String totalIterations= Optional.ofNullable(System.getProperty("count"))
                .filter(s->!StringUtils.isEmpty(s))
                .orElse("1000");

        log.warn("Delay:{},iterations:{}",delay,totalIterations);

        IntStream.range(1,Integer.valueOf(totalIterations)).parallel().forEach(i->{
            try{
                if(random.nextBoolean()){
                    log.info("inserting "+i);
                    insertTradeData();
                }else{
                    log.info("updating "+i);
                    updateTradeData();
                }
                sleep(Long.valueOf(delay));

            }catch (Exception e){
                e.printStackTrace();
            }

        });
        log.warn("All Records inserted");
        System.exit(0);

    }

    private void insertTradeData() throws InterruptedException {
        String name=faker.name().name();
        long id=faker.number().numberBetween(1l,1000000l);
        long traderId=faker.number().numberBetween(1l,1000l);
        String symbol=faker.stock().nsdqSymbol();
        Double shares=faker.number().randomDouble(2,100,500);
        Double price=faker.number().randomDouble(2,100,500);
        String status="OPEN";
        Date date=new Date();
        TradeInfo trade=new TradeInfo(id,symbol,shares,price,traderId,status,date);
        log.info("inserting "+trade);
        boolean success=prodIdQueue.offer(trade,5, TimeUnit.SECONDS);
        tradeInfoMapper.insertTrade(id,symbol,shares,price,date,traderId,status,new Date(),"test");

    }

    private void updateTradeData() throws InterruptedException {
        TradeInfo trade=prodIdQueue.poll(5,TimeUnit.SECONDS);
        if(trade!=null){
            TradeInfo tradeFromDB=tradeInfoMapper.getTrade(trade.getId());
            log.info("Fetched from DB {}",tradeFromDB);
            log.info("updating "+trade);
            Double shares=faker.number().randomDouble(2,100,500);
            Double price=faker.number().randomDouble(2,100,500);
            String status="CLOSED";
            Date date=new Date();
            tradeInfoMapper.updateTrade(trade.getId(),shares,price,date,status,new Date(),"test");
        }
    }

    private void sleep(long duration){
        try {
            System.out.println("Waiting for "+duration);
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
