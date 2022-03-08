package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Main {
    static String str;
    static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy-HH:mm");
    static File binData = new File("D:\\bindata.txt");
    static File roomData = new File("D:\\roomdata.txt");
    static File palletData = new File("D:\\palletdata.txt");

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
                    if(!(inputData[3] =="room")){ //checks the
                        addBinToBay(inputData);
                    }else{
                        addBinToRoom(inputData);
                    }

                }else if(Integer.parseInt(inputData[1])==1){  // 1 = Closed skid, resale
                    addClosedToBay(inputData);


                }else if(Integer.parseInt(inputData[1])==2){ // 2 = scrap

                }

            }else if(Integer.parseInt(inputData[0])==1){
                //office client requests/submits
            }



            System.out.println("Client Detached");
        }
    }

    private static void addClosedToBay(String[] inputData) throws IOException  {
        String[] bayParse = inputData[3].split("\\-");
        int ID = Integer.parseInt(inputData[2]);
        String aisle = bayParse[0].toUpperCase(Locale.ROOT);
        int bay = Integer.parseInt(bayParse[1]);
        PrintWriter PW = new PrintWriter(palletData);

        LocalDateTime now = LocalDateTime.now();
        String time = dtf.format(now);
        PW.println(aisle + " " + bay + " " + ID + " " +  " " + time);
        PW.flush();
        PW.close();
        System.out.println(str);
        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println(1);
        pr.flush();
    }

    static void addBinToBay(String[] input) throws IOException {
        String[] binParse = input[2].split("\\-");
        String[] bayParse = input[3].split("\\-");
        int job = Integer.parseInt(binParse[0]);
        int bin = Integer.parseInt(binParse[1]);
        String aisle = bayParse[0].toUpperCase(Locale.ROOT);
        int bay = Integer.parseInt(bayParse[1]);
        PrintWriter PW = new PrintWriter(binData);

        LocalDateTime now = LocalDateTime.now();
        String time = dtf.format(now);
        PW.println(aisle + " " + bay + " " + job + " " + bin + " " + time);
        PW.flush();
        PW.close();
        System.out.println(str);
        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println(1);
        pr.flush();
    }
    static void addBinToRoom(String[] input) throws IOException {
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

    }


}
