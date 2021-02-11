package clientPart1;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientRunner {
    public static void main(String[] args) {

        CountDownLatch threeHourGate = new CountDownLatch(1);
        CountDownLatch fiveHourGate = new CountDownLatch(1);
        
        AtomicBoolean reachThree = new AtomicBoolean(false);
        AtomicBoolean reachFive  = new AtomicBoolean(false);
        AtomicInteger totalRequest = new AtomicInteger(0);
        AtomicInteger successRequest = new AtomicInteger(0);

        // get input from console
        final String INPUT_HINT = "Please input the following contents according to hints, separate them with ',':\n" +
                "maxStores, customerPerStore, maxItemID, numOfPurchasePerHour, numOfItemPerPurchase, date, serverIP";

        Scanner sc = new Scanner(System.in);
        System.out.println(INPUT_HINT);
        String[] input = sc.nextLine().split(",");

        // set default values
        int maxStores = 32;
        int customerPerStore = 1000;
        int maxItemID = 100000;
        int numOfPurchasePerHour = 60;
        int numOfItemPerPurchase = 5;
        String date = "20210101";
        String serverIP = "http://ec2-107-21-83-193.compute-1.amazonaws.com:8080/homework_war_archive";

        // validate input
        if(input == null || input.length == 0){
            System.out.println("Invalid Input, Exit");
            System.exit(-1);
        }
        try{
            maxStores = Integer.parseInt(input[0]);
            customerPerStore = Integer.parseInt(input[1]);
            maxItemID = Integer.parseInt(input[2]);
            numOfPurchasePerHour = Integer.parseInt(input[3]);
            numOfItemPerPurchase = Integer.parseInt(input[4]);
            if(input[5].matches("[0-9]+") && isDateValid(input[5])){
                date = input[5];
            }
            if(!input[6].equals(" ")){
                serverIP = "http://ec2-107-21-83-193.compute-1.amazonaws.com:8080/homework_war_archive";
            }
        }catch (NumberFormatException e){
            e.printStackTrace();
            System.out.println("Invalid Input, Exit");
            System.exit(-1);
        }

        CountDownLatch allStoresClosed = new CountDownLatch(maxStores);

        Instant startTime = staggeredOpening(threeHourGate, fiveHourGate, allStoresClosed, maxStores, customerPerStore, maxItemID, numOfPurchasePerHour,
                numOfItemPerPurchase, date, serverIP, reachThree, reachFive, totalRequest, successRequest);

        Instant endTime = null;
        try {
            allStoresClosed.await();
            endTime = Instant.now();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Duration timeElapsed = Duration.between(startTime, endTime);

        System.out.println("The day is over, today's statistics:");
        System.out.println("Total successful requests: " + successRequest.get());
        System.out.println("Total unsuccessful requests: " + (totalRequest.get() - successRequest.get()));
        System.out.println("Wall time in seconds: " + timeElapsed.getSeconds());
        System.out.println("The throughput: " + (totalRequest.doubleValue() / timeElapsed.getSeconds()));
    }

    public static Instant staggeredOpening(CountDownLatch threeHourGate, CountDownLatch fiveHourGate, CountDownLatch allStoresClosed,
                                           int maxStores, int customerPerStore, int maxItemID, int numOfPurchasePerHour,
                                           int numOfItemPerPurchase, String date, String serverIP,
                                           AtomicBoolean reachThree, AtomicBoolean reachFive,
                                           AtomicInteger totalRequest, AtomicInteger successRequest){
        int halfStores = maxStores / 2;
        int quarterStores = maxStores / 4;
        int openedStoresCount = 0;

        // east phase stores open
        System.out.println("----East stores open!----");
        Instant startTime = Instant.now();
        launchStores(quarterStores, openedStoresCount, customerPerStore, maxItemID, numOfPurchasePerHour,
                numOfItemPerPurchase, date, serverIP,
                threeHourGate, fiveHourGate, allStoresClosed, reachThree, reachFive,
                totalRequest, successRequest);
        openedStoresCount += quarterStores;

        // central phase stores open
        try {
            threeHourGate.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("----Central stores open!----");
        launchStores(halfStores, openedStoresCount, customerPerStore, maxItemID, numOfPurchasePerHour,
                numOfItemPerPurchase, date, serverIP,
                threeHourGate, fiveHourGate, allStoresClosed, reachThree, reachFive,
                totalRequest, successRequest);
        openedStoresCount += halfStores;

        // west phase stores open
        try {
            fiveHourGate.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("----West stores open!----");
        launchStores(quarterStores, openedStoresCount, customerPerStore, maxItemID, numOfPurchasePerHour,
                numOfItemPerPurchase, date, serverIP,
                threeHourGate, fiveHourGate, allStoresClosed, reachThree, reachFive,
                totalRequest, successRequest);
        openedStoresCount += quarterStores;
        return startTime;
    }

    public static void launchStores(int num, int openedStoresCount, int customerPerStore, int maxItemID,
                                   int numOfPurchasePerHour, int numOfItemPerPurchase, String date, String serverIP,
                                   CountDownLatch threeHourGate, CountDownLatch fiveHourGate, CountDownLatch allStoresClosed,
                                   AtomicBoolean reachThree, AtomicBoolean reachFive, AtomicInteger totalRequest, AtomicInteger successRequest){
        for(int i = 0; i < num; i++){
            StoreThread store = new StoreThread(openedStoresCount + i + 1, customerPerStore, maxItemID,
                    numOfPurchasePerHour, numOfItemPerPurchase, date, serverIP,
                    threeHourGate, fiveHourGate, allStoresClosed, reachThree, reachFive,
                    totalRequest, successRequest);
            Thread thread = new Thread(store);
            thread.start();
        }
    }

    private static boolean isDateValid(String dateStr){
        DateTimeFormatter dtf = DateTimeFormatter.BASIC_ISO_DATE;
        try{
            LocalDate.parse(dateStr, dtf);
        }catch (DateTimeParseException e){
            return false;
        }
        return true;
    }
}
