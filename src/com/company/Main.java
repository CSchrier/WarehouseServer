package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;

public class Main {
    static String str;
    static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy-HH:mm");
    static File palletData = new File("D:\\palletdata.txt");
    static File bayData = new File("/home/keystone/Documents/data.txt");
    static Scanner scan;
    static String time;
    static String clientIP;

    static Socket s;
    public static void main(String[] args) throws IOException {
	    ServerSocket ss = new ServerSocket(4999);


        String aisle;
        int bay;
        int job;
        int bin;
        String time;
        String line = "";
        String[] inputData;
        System.out.println("I am alive!");
        while(true){
            s = ss.accept();
            clientIP = s.getInetAddress().toString();
            System.out.println("Client Connected");
            InputStreamReader in = new InputStreamReader(s.getInputStream());
            BufferedReader br = new BufferedReader(in);
            String str = br.readLine();
            inputData = str.split("\\s");



            if(Integer.parseInt(inputData[0])==0){ //warehouse client requests/submits
                //examples of expected warehouse client data
                //0 0 59059-1 A-101
                //0 1 4909 a-101
                //0 2 steel a-101
                if(Integer.parseInt(inputData[1])==0){  // 0 = bin/skid to be audited
                    if(!(inputData[3].equals("room"))){ //checks the
                        addBinToBay(inputData);
                    }else{
                        addBinToRoom(inputData);
                    }

                }else if(Integer.parseInt(inputData[1])==1){  // 1 = Closed skid, resale
                    if(inputData[3].equals("picked")){
                        pickGood(inputData);
                    }else{
                        addFinishedGoodsToBay(inputData);
                    }



                }else if(Integer.parseInt(inputData[1])==2){ // 2 = scrap
                    addScrapToBay(inputData);
                }

            }else if(Integer.parseInt(inputData[0])==1){
                if(Integer.parseInt(inputData[1])==0){
                    sendOfficeData();
                }else if(Integer.parseInt(inputData[1])==1){  // 1 = Closed skid, resale
                    clearRoomData();
                }else if(Integer.parseInt(inputData[1])==2){
                    returnJobs();
                }
                else if(Integer.parseInt(inputData[1])==3){
                    returnLocations(Integer.parseInt(inputData[2]));
                }
                
            }



            System.out.println("Client Detached");
        }
    }

    private static void returnLocations(int parseInt) throws IOException {
        LinkedList<String> locations = new LinkedList<>();
        scan = new Scanner(bayData);
        String string;
        BufferedReader br = new BufferedReader(new FileReader(bayData));
        while((string = br.readLine()) != null) {


            String[] stringParse = string.split("\\s");
            String job = stringParse[2];
            int stringJob = Integer.parseInt(job);
            if(stringJob==parseInt){
                locations.add(stringParse[0]+stringParse[1]);
            }


        }
        br.close();
        StringBuffer buffer = new StringBuffer();

        for(int i = 0; i< locations.size();i++) {
            buffer.append(locations.get(i)+ " ");
        }
        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println(buffer);
        pr.flush();
        pr.close();


    }

    private static void returnJobs() throws IOException {
        LinkedList<Integer> jobs = new LinkedList<>();

        scan = new Scanner(bayData);
        String string;
        BufferedReader br = new BufferedReader(new FileReader(bayData));
        while((string = br.readLine()) != null) {


            String[] stringParse = string.split("\\s");
            String job = stringParse[2];
            int stringJob = Integer.parseInt(job);
            if(!jobs.contains(stringJob)&&stringJob!=0){
                jobs.add(stringJob);
            }

        }
        br.close();

        int[] sortedJobs = new int[jobs.size()];

        for(int i = 0;i<jobs.size();i++) {
            sortedJobs[i]=jobs.get(i);

        }
        StringBuffer buffer = new StringBuffer();
        Arrays.sort(sortedJobs);
        for(int i = 0; i<sortedJobs.length;i++) {
            buffer.append(sortedJobs[i]);
        }
        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println(buffer);
        pr.flush();
        pr.close();


    }

    private static void pickGood(String[] inputData) throws IOException {
        time = getTime();
        System.out.println("Receiving picked finished goods update from "+clientIP+" at " + time);

        int ID = Integer.parseInt(inputData[2]);




        scan = new Scanner(bayData);


        String string;


        BufferedReader br = new BufferedReader(new FileReader(bayData));
        while((string = br.readLine()) != null) {

            scan = new Scanner(string);
            if(scan.findInLine(String.valueOf(ID))!=null) {
                break;
            }

        }
        br.close();


        if(string != null){
            String[] stringParse = string.split("\\s");




            scan = new Scanner(bayData);
            StringBuffer buffer = new StringBuffer();

            while (scan.hasNextLine()) {
                buffer.append(scan.nextLine()+System.lineSeparator());
            }
            String fileContents = buffer.toString();
            scan.close();
            fileContents = fileContents.replaceAll(string,stringParse[0]+" "+stringParse[1]+" 0 0 0");
            FileWriter writer = new FileWriter(bayData);
            writer.append(fileContents);
            writer.flush();
            writer.close();
        }
    }

    private static void addScrapToBay(String[] inputData) {
        //This will be added at a later date when the details are nailed down
        //the plan at the moment will be to copy the input from something like finished goods
        //not have it overwrite as essentially we will be filling up multiple bays with the same
        //scrap, like 8 bays may just say "steel"
        //We'll be  able to  assign them freely to bays and will clear them by scanning the bay slot
        //and either hitting a clear prompt or scan a barcode on the forklift
    }

    private static void clearRoomData() throws IOException {
        time = getTime();
        System.out.println("responding to office's clear audit room request from "+clientIP+" at " + time);
        scan = new Scanner(bayData);
        StringBuffer buffer = new StringBuffer();

        while (scan.hasNextLine()) {
            str = scan.nextLine();
            String[] size = str.split("\\s");
            if(size.length!=3) {
                buffer.append(str+System.lineSeparator());
            }

        }
        String fileContents = buffer.toString();
        scan.close();

        FileWriter writer = new FileWriter(bayData);
        writer.append(fileContents);
        writer.flush();
        writer.close();
        System.out.println("Audit Room Data Cleared");

    }

    private static void sendOfficeData() throws IOException {
        time = getTime();

        System.out.println("responding to office's data request from " +clientIP+ " at " + time);
        scan = new Scanner(bayData);
        StringBuffer buffer = new StringBuffer();

        while (scan.hasNextLine()) {
            buffer.append(scan.nextLine()+System.lineSeparator());
        }
        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println(buffer);
        pr.flush();
        pr.close();
        System.out.println("Office Data sent");
    }

    private static String getTime() {
         LocalDateTime now = LocalDateTime.now();    //sets date and time
        return dtf.format(now);
    }

    private static void addFinishedGoodsToBay(String[] inputData) throws IOException  {

        time = getTime();
        System.out.println("Receiving finished goods update from " + clientIP + " at " + time);
        String[] bayParse = inputData[3].split("\\-");
        int ID = Integer.parseInt(inputData[2]);
        String aisle = bayParse[0].toUpperCase(Locale.ROOT);
        int bay = Integer.parseInt(bayParse[1]);



        scan = new Scanner(bayData);


        String string;


        BufferedReader br = new BufferedReader(new FileReader(bayData));
        while((string = br.readLine()) != null) {

            scan = new Scanner(string);
            if(scan.findInLine(String.valueOf(ID))!=null) {
                break;
            }

        }
        br.close();


        if(string != null){
            String[] stringParse = string.split("\\s");




            scan = new Scanner(bayData);
            StringBuffer buffer = new StringBuffer();

            while (scan.hasNextLine()) {
                buffer.append(scan.nextLine()+System.lineSeparator());
            }
            String fileContents = buffer.toString();
            scan.close();
            fileContents = fileContents.replaceAll(string,stringParse[0]+" "+stringParse[1]+" 0 0 0");
            FileWriter writer = new FileWriter(bayData);
            writer.append(fileContents);
            writer.flush();
            writer.close();
        }
        //upper limit

        String bayToFind = aisle + " " + bay;
        br = new BufferedReader(new FileReader(bayData));
        while((string = br.readLine()) != null) {

            scan = new Scanner(string);
            if(scan.findInLine(bayToFind)!=null) {
                break;
            }

        }
        br.close();

        scan = new Scanner(bayData);
        StringBuffer buffer = new StringBuffer();

        while (scan.hasNextLine()) {
            buffer.append(scan.nextLine()+System.lineSeparator());
        }
        String fileContents = buffer.toString();
        scan.close();


        PrintWriter PW = new PrintWriter(bayData);


        fileContents = fileContents.replaceAll(string,aisle + " " + bay + " " + ID + " "  + time);





        FileWriter writer = new FileWriter(bayData);
        writer.append(fileContents);
        writer.flush();
        writer.close();
        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println(1);
        pr.flush();





    }

    static void addBinToBay(String[] input) throws IOException {
        time = getTime();
        System.out.println("Receiving bin update from "+clientIP+" at " + time);
        String[] binParse = input[2].split("\\-");
        String[] bayParse = input[3].split("\\-");
        int job = Integer.parseInt(binParse[0]);
        int bin = Integer.parseInt(binParse[1]);
        String aisle = bayParse[0].toUpperCase(Locale.ROOT);
        int bay = Integer.parseInt(bayParse[1]);
        System.out.println("Bin " + bin + "added to bay "+ bay);

        //first we must clear the bay the bin MAY have been in
        scan = new Scanner(bayData);


        String string;
        String binToFind = job + " " + bin;

        BufferedReader br = new BufferedReader(new FileReader(bayData));
        while((string = br.readLine()) != null) {

            scan = new Scanner(string);
            if(scan.findInLine(binToFind)!=null) {
                break;
            }

        }
        br.close();


        if(string != null){
            String[] stringParse = string.split("\\s");




            scan = new Scanner(bayData);
            StringBuffer buffer = new StringBuffer();

            while (scan.hasNextLine()) {
                buffer.append(scan.nextLine()+System.lineSeparator());
            }
            String fileContents = buffer.toString();
            scan.close();
            fileContents = fileContents.replaceAll(string,stringParse[0]+" "+stringParse[1]+" 0 0 0");
            FileWriter writer = new FileWriter(bayData);
            writer.append(fileContents);
            writer.flush();
            writer.close();
        }//end of clearing old bay info portion

        //time to set the bin in the new/first bay

        String bayToFind = aisle + " " + bay;
        br = new BufferedReader(new FileReader(bayData));
        while((string = br.readLine()) != null) {

            scan = new Scanner(string);
            if(scan.findInLine(bayToFind)!=null) {
                break;
            }

        }
        br.close();

        scan = new Scanner(bayData);
        StringBuffer buffer = new StringBuffer();

        while (scan.hasNextLine()) {
            buffer.append(scan.nextLine()+System.lineSeparator());
        }
        String fileContents = buffer.toString();
        scan.close();


        PrintWriter PW = new PrintWriter(bayData);

        if(string!=null) {
            fileContents = fileContents.replaceAll(string, aisle + " " + bay + " " + job + " " + bin + " " + time);
        }else{
            System.out.println("null replacement string sent");
        }




        FileWriter writer = new FileWriter(bayData);
        writer.append(fileContents);
        writer.flush();
        writer.close();
        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println(1);
        pr.flush();
        System.out.println("Bin Update Successful");
    }
    static void addBinToRoom(String[] input) throws IOException {
        /*
        String[] binParse = input[2].split("\\-");
        int job = Integer.parseInt(binParse[0]);
        int bin = Integer.parseInt(binParse[1]);
        PrintWriter PW = new PrintWriter(roomData);
        LocalDateTime now = LocalDateTime.now();
        String time = dtf.format(now);
        PW.println(job + " " + bin + " " + time);
        PW.flush();
        PW.close();
        System.out.println(str);
        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println(1);
        pr.flush();

         */
        time = getTime();
        System.out.println("Receiving bin to room update from warehouse at " + time);
        String[] binParse = input[2].split("\\-");
        int job = Integer.parseInt(binParse[0]);
        int bin = Integer.parseInt(binParse[1]);


        //first we must clear the bay the bin MAY have been in
        scan = new Scanner(bayData);


        String string;
        String binToFind = job + " " + bin;

        BufferedReader br = new BufferedReader(new FileReader(bayData));
        while((string = br.readLine()) != null) {

            scan = new Scanner(string);
            if(scan.findInLine(binToFind)!=null) {
                break;
            }

        }
        br.close();


        if(string != null){
            String[] stringParse = string.split("\\s");




            scan = new Scanner(bayData);
            StringBuffer buffer = new StringBuffer();

            while (scan.hasNextLine()) {
                buffer.append(scan.nextLine()+System.lineSeparator());
            }
            String fileContents = buffer.toString();
            scan.close();
            fileContents = fileContents.replaceAll(string,stringParse[0]+" "+stringParse[1]+" 0 0 0");
            FileWriter writer = new FileWriter(bayData);
            writer.append(fileContents);
            writer.flush();
            writer.close();
        }//end of clearing old bay info portion

        //time to set the bin in the new/first bay




        scan = new Scanner(bayData);
        StringBuffer buffer = new StringBuffer();

        while (scan.hasNextLine()) {
            buffer.append(scan.nextLine()+System.lineSeparator());
        }
        buffer.append(job + " " + bin + " " + time);
        String fileContents = buffer.toString();
        scan.close();

        FileWriter writer = new FileWriter(bayData);
        writer.append(fileContents);
        writer.flush();
        writer.close();
        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println(1);
        pr.flush();
        System.out.println("Bin Update Successful");


    }


}
