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

    static File logFile = new File("/home/keystone/Documents/log.txt");
    static Scanner scan;
    static String time;
    static String clientIP;

    static Socket s;
    public static void main(String[] args) throws IOException {

        //Opens socket 4999 on application start
        ServerSocket ss = new ServerSocket(4999);

        //initialize string array for input data
        String[] inputData;

        //console output for server start
        System.out.println("Server started!");

        //main system loop, waits on socket messages
        while(true){

            //accepts socket connection and records client IP and time
            s = ss.accept();
            clientIP = s.getInetAddress().toString();
            time = getTime();

            System.out.println(time + " - Incoming Client Connection");

            //reads client input message and splits the message into the inputData array
            InputStreamReader in = new InputStreamReader(s.getInputStream());
            BufferedReader br = new BufferedReader(in);
            String str = br.readLine();
            inputData = str.split("\\s");


            //function decision tree
            //first block for warehouse clients
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

                }

                else if(Integer.parseInt(inputData[1])==1){  // 1 = Closed skid, resale
                    if(inputData[3].equals("picked")){
                        pickGood(inputData);
                    }else{
                        addFinishedGoodsToBay(inputData);
                    }
                }

                else if(Integer.parseInt(inputData[1])==2){ // 2 = scrap
                    addScrapToBay(inputData);
                }

                else if(Integer.parseInt(inputData[1])==3){ // 3 = aisle audit request
                    String aisle = inputData[2];
                    replyToAndroidAuditRequest(aisle);
                }


            //second block for responding to database queries from warehouse clients
            }else if(Integer.parseInt(inputData[0])==1){


                if(Integer.parseInt(inputData[1])==0){
                    desktopClientDataDump();


                }else if(Integer.parseInt(inputData[1])==1){  // 1 = Closed skid, resale
                    clearRoomData();


                }else if(Integer.parseInt(inputData[1])==2){
                    returnJobs();
                }

                //returns location of an android client queried job
                else if(Integer.parseInt(inputData[1])==3){
                    replyToAndroidLocationRequest(Integer.parseInt(inputData[2]));
                }
                
            }else{
                continue;
            }


            time = getTime();
            System.out.println(time + " - Client successfully responded\n\n");
        }
    }

    //function takes an aisle request from associated android app and replies with every bay of the given aisles contents
    private static void replyToAndroidAuditRequest(String aisle) throws IOException {

        LinkedList<String> aisleBays = new LinkedList<>(); //initialize linkedlist to be filled bays from the searched aisle
        scan = new Scanner(bayData);                       //initialize scanner for database
        String string;                                     //temp holding string

        BufferedReader br = new BufferedReader(new FileReader(bayData));  //starts a bufferedreader pointing at database


        while((string = br.readLine()) != null) { //loops through database lines while they exist
            //example line
            //A 204 59053 4 02/01/2022-09:55
            String[] stringParse = string.split("\\s");    //breaks apart database line into seperate elements of an array
            String aisleParse = stringParse[0];       //gets the aisle string from database line
            if(aisle==aisleParse){    //compares the aisle to our searched for aisle
                aisleBays.add(stringParse[0]+"/"+stringParse[1]+"/"+stringParse[2]+"/"+stringParse[3]);  //adds the aisle, bay, job#, and bin# to the aisleBays linkedlist
            }


        }
        br.close();


        StringBuffer aisleContentsOutput = new StringBuffer(); //stringbuffer to hold our output message of AisleContents

        for(int i = 0; i< aisleBays.size();i++) {         //walks the list
            aisleContentsOutput.append(aisleBays.get(i)+ " "); //append locations to our text output
        }
        PrintWriter replyToClient = new PrintWriter(s.getOutputStream()); //open an output stream to our connected client

        replyToClient.println(aisleContentsOutput); //send our client the locations of their searched bins
        replyToClient.flush(); //make sure the whole reply has sent
        replyToClient.close(); //close writer

    }


    //function reply to android location request takes a job number and returns the locations of that job to the requesting client
    private static void replyToAndroidLocationRequest(int searchedJob) throws IOException {


        LinkedList<String> locations = new LinkedList<>(); //initialize linkedlist to be filled with locations
        scan = new Scanner(bayData);                       //initialize scanner for database
        String string;                                     //temp holding string

        BufferedReader br = new BufferedReader(new FileReader(bayData));  //starts a bufferedreader pointing at database


        while((string = br.readLine()) != null) { //loops through database lines while they exist
            //example line
            //A 204 59053 4 02/01/2022-09:55
            String[] stringParse = string.split("\\s");    //breaks apart database line into seperate elements of an array
            String job = stringParse[2];       //gets the job number string from database line
            int stringJob = Integer.parseInt(job);  //converts job number string to an int
            if(stringJob==searchedJob){    //compares the job number to our searched number
                locations.add(stringParse[0]+stringParse[1]);  //adds the aisle and bay to the locations linkedlist
            }


        }
        br.close();


        StringBuffer locationOutput = new StringBuffer(); //stringbuffer to hold our output message of locations

        for(int i = 0; i< locations.size();i++) {         //walks the list
            locationOutput.append(locations.get(i)+ " "); //append locations to our text output
        }
        PrintWriter replyToClient = new PrintWriter(s.getOutputStream()); //open an output stream to our connected client

        replyToClient.println(locationOutput); //send our client the locations of their searched bins
        replyToClient.flush(); //make sure the whole reply has sent
        replyToClient.close(); //close writer


    }

    private static void writeToLog(String messageToLog) throws IOException {
        time = getTime();
        FileWriter fw = new FileWriter(logFile, true);
        fw.write(time +" - " + messageToLog+"\n");
        fw.close();
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
            buffer.append(sortedJobs[i]+ " ");
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


    private static void desktopClientDataDump() throws IOException {  //replies to desktop client's request for all bay data

        time = getTime(); //get current time

        System.out.println("responding to desktop client " +clientIP+ " data request at " + time); //system cli output
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

        System.out.println("Receiving bin update from "+clientIP);

        //note - expected data looks like:
        // 0 0 (job)-(bin) (aisle)-(bay)
        String[] binParse = input[2].split("\\-"); //breaks apart the job-bin input
        String[] bayParse = input[3].split("\\-"); //breaks apart the aisle-bay input

        //assigns the individual parts of the above job bin aisle and bay to variables
        int job = Integer.parseInt(binParse[0]);
        int bin = Integer.parseInt(binParse[1]);
        String aisle = bayParse[0].toUpperCase(Locale.ROOT);
        int bay = Integer.parseInt(bayParse[1]);


        System.out.println("Attempting to add " + input[2] + " to " + input[3]);

        //first we must clear the bay the bin MAY have been in case we're moving one location to another
        scan = new Scanner(bayData); //attach data file


        String currentLine; //intialize temp string to hold current line in database
        String binToFind = job + " " + bin; //create a string in the format of the database of the job and bin

        //create a buffered reader of the file
        BufferedReader br = new BufferedReader(new FileReader(bayData));
        while((currentLine = br.readLine()) != null) { //load lines 1 by 1 of the database until there aren't anymore

            scan = new Scanner(currentLine); //attach the current line to a scanner
            if(scan.findInLine(binToFind)!=null) { //check for our searched text within the line, returns null if not found
                break; //break if we find the line with our search, may not exist
            }

        }
        br.close();


        if(currentLine != null){ //if we found the text

            String[] stringParse = currentLine.split("\\s"); //create an array to break apart the line containing our searched data

            scan = new Scanner(bayData);
            StringBuffer buffer = new StringBuffer();

            while (scan.hasNextLine()) { //loads the entire database into a string
                buffer.append(scan.nextLine()+System.lineSeparator());
            }

            String fileContents = buffer.toString();//creates a string out of the loaded buffer
            scan.close();

            //takes the line we found to have the bin that is being moved and replaces it with blanked info then writes to file
            fileContents = fileContents.replaceAll(currentLine,stringParse[0]+" "+stringParse[1]+" 0 0 0");
            FileWriter writer = new FileWriter(bayData);
            writer.append(fileContents);
            writer.flush();
            writer.close();
        }//end of clearing old bay info portion

        //time to set the bin in the new/first bay

        String bayToFind = aisle + " " + bay;
        br = new BufferedReader(new FileReader(bayData));
        while((currentLine = br.readLine()) != null) {

            scan = new Scanner(currentLine);
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



        if(currentLine!=null) {
            String[] overwrittenContent = currentLine.split("\\s");
            writeToLog(overwrittenContent[2]+"-"+overwrittenContent[3]+" from " + overwrittenContent[0]+"-"+overwrittenContent[1]+" to be overwritten");
            fileContents = fileContents.replaceAll(currentLine, aisle + " " + bay + " " + job + " " + bin + " " + time);
        }else{
            System.out.println("Error: "+aisle+"-"+bay+" not found");
        }

        FileWriter writer = new FileWriter(bayData);
        writer.append(fileContents);
        writer.flush();
        writer.close();
        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println(1);
        pr.flush();
        System.out.println("Bin Update Successful");
        writeToLog(job+"-"+bin+" to "+aisle+"-"+bay);
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
