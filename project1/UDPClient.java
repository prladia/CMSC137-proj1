/* Primzen Rowena Ladia
* CMSC 137 CD-2L
* References:
* https://systembash.com/a-simple-java-udp-server-and-udp-client/
* http://www.inetdaemon.com/tutorials/internet/tcp/3-way_handshake.shtml
* http://www.tcpipguide.com/free/t_TCPdataEstablishmentProcessTheThreeWayHandsh-3.htm
* http://www.inetdaemon.com/tutorials/internet/udp/
* http://www.inetdaemon.com/tutorials/internet/tcp/tcp_header.shtml
* http://www.programmershare.com/3509695/
* http://www.tcpipguide.com/free/t_TCPConnectionTermination-2.htm
*/
import java.util.*;
import java.io.*;
import java.net.*;

public class UDPClient {

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

	public static void printData(int synnum, int acknum, int synbit, int ackbit, int finbit, int window, String header, String data){

			System.out.println("SYN number: " + synnum);
			System.out.println("ACK number: " + acknum);
			System.out.println("SYN bit: " + synbit);
			System.out.println("ACK bit: " + ackbit);
			System.out.println("FIN bit: " + finbit);
			System.out.println("Window: " + window);
			//System.out.println(header);
	}

	public static void main(String args[]) throws Exception {

		int choice;

		int flag;	//1 - sent; 0 - not sent
		int disconnect;
		int random;

		String data = "C";
		String header;
		

		double chancedrop;

		BufferedReader fromUser = new BufferedReader(new InputStreamReader (System.in));
		DatagramSocket clientsocket = new DatagramSocket();
		DatagramPacket sendPacket;
		DatagramPacket receivePacket;

		InetAddress ipaddress = InetAddress.getByName("localhost");
		int port = 9090;

		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		Random randomizer = new Random();

		do{
			flag = 0;	//not yet sent
			choice = randomizer.nextInt(4);		

			//randomize chance of packet drops
			switch(choice){
				case 1:
					chancedrop = 0.25;
				break;
				case 2:
					chancedrop = 0.50;
				break;
				case 3:
					chancedrop = 0.75;
				break;
				default:
					chancedrop = 0;
				break;
			}

			if(chancedrop == 0 || chancedrop <= randomizer.nextDouble()){
				flag = 1;	//sent
			}

			synno = randomizer.nextInt(100);	//this is just to generate a SYN number for the client to send to the SERVER
			synbit = 1;	//means the header has SYN
			
			//convert to string with the appropriate offset in the header
			convertToStringData();
			
			header = data + synnos + acknos + synbits + ackbits + finbits + windows;

			System.out.println("Data to be sent:");
			printData(synno, ackno, synbit, ackbit, finbit, window, header, data);

			sendData = header.getBytes();	//convert the header in bytes

			//convert the data to a datagram packet that would be sent
			sendPacket = new DatagramPacket(sendData, sendData.length, ipaddress, port);
			
			if(flag == 1){	//Send SYN to server
				System.out.println("REQUEST SENT TO THE SERVER");
				Thread.sleep(1000);
				clientsocket.send(sendPacket);
				Thread.sleep(1000);
			}
			else{
				Thread.sleep(3000);
				System.out.println("ERROR. NETWORK TIMEOUT");
				System.out.println("Resending request to server. . .");
				Thread.sleep(3000);
				
				random = randomizer.nextInt(2);
				if(random == 1){
					data = "D";
					break;
				}
				else{
					flag = 1;
					clientsocket.send(sendPacket);
				}
			}

			//Server listened to request of client
			receivePacket = new DatagramPacket(receiveData, receiveData.length);

			clientsocket.receive(receivePacket);

			receivedMessage = new String(receivePacket.getData());
			System.out.println("RECEIVED FROM THE SERVER");

			//Convert data header to read integer
			//FORMAT OF READING BASED ON CONNECT___XXXXXXXXXXXXXXX
			data = receivedMessage.substring(0, 1);
			convertToIntegerData();

			System.out.println("Data received from server:");
			printData(synno, ackno, synbit, ackbit, finbit, window, header, data);
			System.out.println("Signal: C");
			synbit = 0;
			ackbit = 1;
			int temp = synno;

			synno = ackno;
			ackno = temp + 1;

			//convert to string
			convertToStringData();

			header = data + synnos + acknos + synbits + ackbits + finbits + windows;
			sendData = header.getBytes();
			sendPacket = new DatagramPacket(sendData, sendData.length, ipaddress, port);

			System.out.println("Sending data to server. . .");
			clientsocket.send(sendPacket);

			synno = ackno = 0;
			
			synbit = ackbit = finbit = 0;

			disconnect = randomizer.nextInt(10);

			window = randomizer.nextInt(100);

			if(3 > disconnect){
				data = "D";
			}
		} while(data != "D");

		//DISCONNECTION
		Thread.sleep(2000);

		finbit = 1;		//Send fin bit to close connection

		synno = randomizer.nextInt(100);
		convertToStringData();

		header = "D" + synnos + acknos + synbits + ackbits + finbits + windows;

		System.out.println("Send signal for disconnection. . .");
		printData(synno, ackno, synbit, ackbit, finbit, window, header, data);
		System.out.println("Signal: D");
		sendData = header.getBytes();

		sendPacket = new DatagramPacket(sendData, sendData.length, ipaddress, port);

		clientsocket.send(sendPacket);

		//client receives the ACK for its FIN. It must now wait for the server to close.

		//client receives FIN bit for server
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientsocket.receive(receivePacket);
		System.out.println("Server ready for disconnection");

		receivedMessage = new String(receivePacket.getData());

		data = receivedMessage.substring(0, 1);
		convertToIntegerData();

		printData(synno, ackno, synbit, ackbit, finbit, window, header, data);
		System.out.println("Signal: D");
		// client receives the server's FIN and sends back an ACK.
		ackno = synno + 1;

		synno = randomizer.nextInt(100);
		convertToStringData();

		header = "D" + synnos + acknos + synbits + ackbits + finbits + windows;

		System.out.println("Client ready to disconnect");
		printData(synno, ackno, synbit, ackbit, finbit, window, header, data);
		System.out.println("Signal: D");
		//client sends ACK to server
		sendData = header.getBytes();
		sendPacket = new DatagramPacket(sendData, sendData.length, ipaddress, port);

		clientsocket.send(sendPacket);

		//client waits for a period of time equal to double the maximum segment life (MSL) time, to ensure the ACK it sent was received.
		Thread.sleep(2000);
		System.out.println("Disconnecting. . .");
		Thread.sleep(10000);
		//The timer expires after double the MSL time.
		//connection closed
		System.out.println("Disonnected successfully");
	}
}