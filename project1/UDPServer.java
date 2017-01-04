/* Primzen Rowena Ladia
* CMSC 137 CD-2L
* References:
* https://systembash.com/a-simple-java-udp-server-and-udp-client/
* http://www.inetdaemon.com/tutorials/internet/tcp/3-way_handshake.shtml
* http://www.tcpipguide.com/free/t_TCPdataEstablishmentProcessTheThreeWayHandsh-3.htm
* http://www.inetdaemon.com/tutorials/internet/udp/
* http://www.inetdaemon.com/tutorials/internet/tcp/tcp_header.shtml
* http://www.programmershare.com/3509695/
*/

import java.util.*;
import java.io.*;
import java.net.*;

public class UDPServer {
	//Since TCP does a three-way handshake, we will need SYN and ACK variables.
		static int synno = 0;
		static int ackno = 0;

		//indication of SYN and ACK number in the header are the SYN and ACK bit
		//FIN bit indication of end of session
		static int synbit = 0;
		static int ackbit = 0;
		static int finbit = 0;

		static int window = 9;

		//to fit the byte size of 1024 and esily access the digits,
		//we make the format: CONNECT___XXXXXXXXXXXXXXX for the DATAGRAM FORMAT
		//X for integer representation of 1-9
		//four digits for synno, ackno, window;
		//one digit for synbit, ackbit, finbit;
		static String synnos, acknos, synbits, ackbits, finbits, windows;
		static String receivedMessage;

	public static void convertToStringData(){
		synnos = String.format("%04d", synno);
		acknos = String.format("%04d", ackno);
		synbits = String.format("%01d", synbit);
		ackbits = String.format("%01d", ackbit);
		finbits = String.format("%01d", finbit);
		windows = String.format("%04d", window);
	}

	public static void convertToIntegerData(){
		synno = Integer.parseInt(receivedMessage.substring(1, 5));
		ackno = Integer.parseInt(receivedMessage.substring(5, 9));
		synbit = Integer.parseInt(receivedMessage.substring(9, 10));
		ackbit = Integer.parseInt(receivedMessage.substring(10, 11));
		finbit = Integer.parseInt(receivedMessage.substring(11, 12));
		window = Integer.parseInt(receivedMessage.substring(12, 16));
	}

	public static void printData(int synnum, int acknum, int synbit, int ackbit, int finbit, int window, String data){

			System.out.println("SYN number: " + synnum);
			System.out.println("ACK number: " + acknum);
			System.out.println("SYN bit: " + synbit);
			System.out.println("ACK bit: " + ackbit);
			System.out.println("FIN bit: " + finbit);
			System.out.println("Window: " + window);
			System.out.println("Connection Status: " + data + "\n");
	}

	public static void main(String args[]) throws Exception {
		
		int choice;

		int flag;	//1 - sent; 0 - not sent
		int disconnect;
		int port;

		String data = "C";
		String header;

		double chancedrop;

		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		Random randomizer = new Random();

		BufferedReader fromUser = new BufferedReader(new InputStreamReader (System.in));
		DatagramSocket serversocket = new DatagramSocket(9090);
		DatagramPacket sendPacket;
		DatagramPacket receivePacket;

		InetAddress ipaddress;

		while(true){

			//receiving SYN
			receivePacket = new DatagramPacket(receiveData, receiveData.length);

			serversocket.receive(receivePacket);
			receivedMessage = new String(receivePacket.getData());	//SYN received from client

			System.out.println("Request from client received. . .");

			//Convert header to integer
			data = receivedMessage.substring(0, 1);
			
			if(data.equals("D")){	//client sent disconnection
				break;
			}

			convertToIntegerData();

			System.out.println("Data received: ");
			printData(synno, ackno, synbit, ackbit, finbit, window, data);

			//initialize variables needed for Three-way handshake connection
			ackno = synno + 1;
			ackbit = 1;
			synbit = 1;

			//convert to string with the appropriate offset in the header
			convertToStringData();
			
			header = data + synnos + acknos + synbits + ackbits + finbits + windows;

			ipaddress = receivePacket.getAddress();
			port = receivePacket.getPort();

			System.out.println("Sending request back to client");
			//sends a single SYN+ACK message back to the client that contains an ACK for the client's SYN, and the server's own SYN
			sendData = header.getBytes();
			sendPacket = new DatagramPacket(sendData, sendData.length, ipaddress, port);
			serversocket.send(sendPacket);	//The server waits for an ACK to the SYN it sent previously.

			//The server receives the ACK to its SYN and is now done with connection establishment.
			receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serversocket.receive(receivePacket);

			header = new String(receivePacket.getData());
			//Convert header to integer
			data = receivedMessage.substring(0, 1);
			convertToIntegerData();
			
			System.out.println("Client has established connection");
			printData(synno, ackno, synbit, ackbit, finbit, window, data);

			System.out.println("\n"+"Server establishing connection. . .");
			System.out.println("Connection susccessful");
		}

		//DISCONNECTION
		//received Fin bit signaling for termination of connection
		System.out.println("Client sent signal for disconnection");
		printData(synno, ackno, synbit, ackbit, finbit, window, data);

		ackno = synno + 1;	//ACK that would be sent back to client to acknowledge FIN
		synno = 0;
		finbit = 0;		//fin bit resets

		//convert to string with the appropriate offset in the header
		convertToStringData();
	
		header = "D" + synnos + acknos + synbits + ackbits + finbits + windows;

		ipaddress = receivePacket.getAddress();
		port = receivePacket.getPort();

		System.out.println("Sending disconnection signal to client. . .");
		////sends an ACK to acknowledge the FIN
		sendPacket = new DatagramPacket(sendData, sendData.length, ipaddress, port);
		serversocket.send(sendPacket);

		//waits for application process on its end to signal close
		finbit = 1;	//server read disconnection from app
		synno = randomizer.nextInt(100);

		//convert to string with the appropriate offset in the header
		convertToStringData();
	
		header = "D" + synnos + acknos + synbits + ackbits + finbits + windows;

		ipaddress = receivePacket.getAddress();
		port = receivePacket.getPort();

		System.out.println("Server is now ready to close");
		//server is now ready to send FIN bit to close
		sendData = header.getBytes();
		sendPacket = new DatagramPacket(sendData, sendData.length, ipaddress, port);
		serversocket.send(sendPacket);

		//The server is waiting for an ACK for the FIN it sent.

		//waiting. . . . .

		//server receives the ACK to its FIN and closes the connection.
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		serversocket.receive(receivePacket);

		header = new String(receivePacket.getData());
		//Convert header to integer
		data = receivedMessage.substring(0, 1);
		convertToIntegerData();
		
		System.out.println("Last data received from Client");
		printData(synno, ackno, synbit, ackbit, finbit, window, data);

		Thread.sleep(1000);
		System.out.println("Disconnected susccessfully");
	}
}