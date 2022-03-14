package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;

public class Main {
    static String str;
    static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy-HH:mm");
    static File binData = new File("D:\\bindata.txt");
    static File roomData = new File("D:\\roomdata.txt");
    static File palletData = new File("D:\\palletdata.txt");
    static File bayData = new File("C:\\Users\\bigwheel\\IdeaProjects\\WarehouseServer\\data.txt");
    static Scanner scan;
    static String time;

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
                }
                
            }



            System.out.println("Client Detached");
        }
    }

    private static void pickGood(String[] inputData) throws IOException {
        time = getTime();
        System.out.println("Receiving picked finished goods update from warehouse at " + time);

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
        System.out.println("responding to office's clear audit room request at " + time);
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
        System.out.println("responding to office's data request at " + time);
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
        System.out.println("Receiving finished goods update from warehouse at " + time);
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
        System.out.println("Receiving bin update from warehouse at " + time);
        String[] binParse = input[2].split("\\-");
        String[] bayParse = input[3].split("\\-");
        int job = Integer.parseInt(binParse[0]);
        int bin = Integer.parseInt(binParse[1]);
        String aisle = bayParse[0].toUpperCase(Locale.ROOT);
        int bay = Integer.parseInt(bayParse[1]);

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


        fileContents = fileContents.replaceAll(string,aisle + " " + bay + " " + job + " " + bin + " " + time);





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
