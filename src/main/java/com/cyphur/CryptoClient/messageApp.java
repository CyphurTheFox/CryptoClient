package com.cyphur.CryptoClient;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author cyphur
 */
import com.cyphur.Crypto.CyphurCrypt;
import com.cyphur.Crypto.handshakeException;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//Java code for thread creation by extending
//the Thread class
class Recieve2 extends Thread {

    Socket sock;
    DataInputStream dIn;
    Send2 send;
    private CyphurCrypt cipher;

    private byte[] recieveData() throws IOException {

        int len = dIn.readInt();
        System.out.println();
//        System.out.print("DataRecvd(" + len + ")...");
        byte[] data = new byte[len];

        dIn.read(data);
//        System.out.println("Transfer of " + len + " bytes Complete!");
        return data;
    }

    public void run() {
        byte[] recieved;
        byte[] message = null;
        try {

            while (!cipher.connectionEstablished) {
                recieved = recieveData();
//                for(byte by : recieved){
//                    System.out.print(by + "|");
//                 }

                try {
                    message = cipher.parseMessage(recieved);
                } catch (handshakeException ex) {
                    Logger.getLogger(Recieve2.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (cipher.sendBack) {
                    send.sendData(message);
                }
                if (cipher.connectionEstablished) {
                    break;
                }
            }
            send.start();

            System.out.println("Connection Established!");
            while (true) {
                recieved = recieveData();

                try {
                    message = cipher.parseMessage(recieved);
                } catch (handshakeException e) {

                }

                String messages = null;
                messages = new String(message);
                System.out.println("<OtherPerson>" + messages);

            }

        } catch (IOException e) {
// Throwing an exception
            e.printStackTrace();
        }
    }

    public Recieve2(Socket sock, Send2 sender, CyphurCrypt cipher) {
        this.sock = sock;
        try {
            this.dIn = new DataInputStream(sock.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(Recieve2.class.getName()).log(Level.SEVERE, null, ex);
        }
        send = sender;
        this.cipher = cipher;
    }
}

class Send2 extends Thread {

    Socket sock;
    DataOutputStream dOut;
    private CyphurCrypt cipher;

    protected void sendData(byte[] data) throws IOException {
        dOut.writeInt(data.length);
        dOut.write(data);
    }

    public void run() {
        Scanner user = new Scanner(System.in);
        String message;
        try {
            while (true) {
                message = user.nextLine();
                byte[] messageb = message.getBytes();
                byte[] transmission = cipher.sendMessage(messageb);
                sendData(transmission);

            }

        } catch (Exception e) {
            // Throwing an exception
            e.printStackTrace();
        }
    }

    public Send2(Socket sock, CyphurCrypt cipher) {
        this.sock = sock;
        this.cipher = cipher;
        try {
            dOut = new DataOutputStream(sock.getOutputStream());
        } catch (IOException e) {

        }

    }
}
//Main Class

public class messageApp {

    public static void main(String[] args) throws IOException {
        // Cyphur Crypt Direct Messaging Command Line Tool
        MainProg:{
        Scanner sysIn = new Scanner(System.in);
        while (true) {
            System.out.print("Type Server IP or press Enter to start a server:");
            String inp = sysIn.nextLine();
            if (inp.isBlank()) {

                System.out.print("Creating Server. . . \nPlease Specify Server Port:");
                int port;

                while (true) {
                    inp = sysIn.nextLine();
                    if (inp.matches("\\d{1,5}")) {
                        port = Integer.parseInt(inp);
                        if (port > 65534) {
                            System.out.print("Invalid Port Number (Out of Range)! Please Supply new Number: ");
                        } else if (port <= 1024) {
                            System.out.println("[WARNING] Selected port is one of the well known ports and should not be used unless you know what you are doing! Are you sure you want to use this port? (y/n)");
                            inp = sysIn.nextLine();
                            if (inp.matches("[yY].*")) {
                                server(port);
                            } else {
                                System.out.print("Creating Server. . . \nPlease Specify Server Port:");
                            }
                        } else {
                            server(port);
                            break MainProg;
                        }

                    } else {
                        System.out.print("Invalid Port Number (Not Number)! Please Supply new Number: ");
                    }
                }
            } else if(inp.matches("^((25[0-5]|(2[0-4]|1[0-9]|[1-9]|)[0-9])(\\.(?!$)|$)){4}$")){
                String IP = inp;
                int port;
                System.out.print("Connecting . . .\n Please Specify Port number:");
                
                 while (true) {
                    inp = sysIn.nextLine();
                    if (inp.matches("[1234567890]{4,}")) {
                        port = Integer.parseInt(inp);
                        if (port > 65534) {
                            System.out.print("Invalid Port Number! Please Supply new Number: ");
                        } else {
                            client(IP,port);
                            break MainProg;
                        }

                    } else {
                        System.out.print("Invalid Port Number! Please Supply new Number: ");
                    }
                }
            } else {
                
            }
            
        }
        }

    }

    public static void client(String IP, int port) throws IOException {
        Socket s = new Socket(IP, port); // port number
        CyphurCrypt crypto = new CyphurCrypt();

        Send2 client = new Send2(s, crypto);

        Recieve2 server = new Recieve2(s, client, crypto);
        server.start();

        System.out.println("Initiating CyphurCrypt Handshake");

        byte[] greeting = crypto.generateGreeting();
        for (byte by : greeting) {
            System.out.print(by + "|");
        }

        client.sendData(greeting);

    }

    public static void server(int Sock) throws IOException {
        ServerSocket ss = new ServerSocket(Sock);
        Socket s = ss.accept();
        CyphurCrypt crypto = new CyphurCrypt();
        System.out.println("Message from Server: client connected");

        Send2 client = new Send2(s, crypto);

        Recieve2 server = new Recieve2(s, client, crypto);
        server.start();
    }
}
