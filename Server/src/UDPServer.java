
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UDPServer implements Runnable {
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd@HH:mm:ss");
	// private static final String SERVER_URL = "./udp/images/";
	private static final String SERVER_URL = System.getProperty("user.dir") + File.separator + "udp"
			+ File.separator + "images" + File.separator;
	private static final int PACKET_SIZE = 8192;
	private int port;
	private DatagramSocket serverSocket = null;
	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;
	private byte[] in;
    private byte[] out;
	
	public UDPServer(int port) {
		this.port = port;
		this.printIPV4();
	}
	
	/**
	 * @param iPAddress adresse du client
	 * @param command commande envoyée par le client
	 */
	private void logger(DatagramPacket packet, String command) {
		String remoteIP = packet.getSocketAddress().toString();
		System.out.println("[" + remoteIP.substring(1, remoteIP.length()) + " - "
				+ UDPServer.dtf.format(LocalDateTime.now()) + "]: " + command);
	}
	
	/**
	 * Imprime l'adresse et le port du server. Meme fonction pour TCP & UDP car agnostic
	 */
	private void printIPV4() {
		try {
			InetAddress inetAddress;
			inetAddress = InetAddress.getLocalHost();
			System.out.println("UDP Server @"+" IP Address:- " 
					+ inetAddress.getHostAddress() + " Port:- " + this.port);
		} catch (UnknownHostException e) {
			System.out.println("Could not obtain IP. Exiting...");
			System.exit(1);
		}

	}
	
	private void setUpUDP() throws IOException {
		this.serverSocket = new DatagramSocket(this.port);
		this.in = new byte[PACKET_SIZE];
        this.out = new byte[PACKET_SIZE];
	}
	
	/**
	 * @param command la commande en byte[]
	 * @return la commande en string
	 */
	public static String fetchCommand(byte[] command) 
    { 
        if (command == null) 
            return null; 
        StringBuilder build = new StringBuilder(); 
        int i = 0; 
        while (command[i] != 0) 
        { 
            build.append((char) command[i]); 
            i++; 
        } 
        return build.toString(); 
    }
	
	/**
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * gère la boucle de commandes d'un client jusqu'à un "back"
	 */
	private void clientLoop() throws IOException, ClassNotFoundException {
		boolean flag = false;
		DatagramPacket receivedPacket = null;
		while (!flag) {
			receivedPacket = new DatagramPacket(in, in.length);
            serverSocket.receive(receivedPacket); 
            InetAddress IPAddress = receivedPacket.getAddress();
            int senderPort = receivedPacket.getPort();
            
            String clientCommand = fetchCommand(in);
            // flush
            in = new byte[PACKET_SIZE];
			String[] terms = clientCommand.split(" ");
			String command = terms[0];

			this.logger(receivedPacket, clientCommand);
			switch (command) {
			case "ls":
				File folder = new File(SERVER_URL);
				File[] listOfFiles = folder.listFiles();
				String fileNames = "";
				// Create a single string containing all the files and folders of the SERVER_URL
				for (File file : listOfFiles) {
					if (file.isFile()) {
						fileNames = fileNames + ("[File]\t\t" + file.getName()) + "!!DELIM!!";
					} else if (file.isDirectory()) {
						fileNames = fileNames + ("[Folder]\t" + file.getName()) + "!!DELIM!!";
					}
				}
				//Send the filenames
				out = fileNames.getBytes();
				sendPacket = new DatagramPacket(out, out.length, IPAddress, senderPort);
                serverSocket.send(sendPacket);
				break;
				
			case "download":
				File file =  new File(SERVER_URL+terms[1]);
				if (!file.exists()) {
					// envoyer un message d'erreur
					String fileSizeStr = "0";
					out = fileSizeStr.getBytes();
				    // envoyer la taille du fichier
				    sendPacket = new DatagramPacket(out, out.length, IPAddress, senderPort);
					serverSocket.send(sendPacket);
					return;
				}
				else {
					//Lire et envoyer tous les bytes du fichier spécifier
					byte[] byteFile = Files.readAllBytes(file.toPath());
					int fileSize = byteFile.length;
					String fileSizeStr = String.valueOf(fileSize);
					out = fileSizeStr.getBytes();
				    // envoyer la taille du fichier
				    sendPacket = new DatagramPacket(out, out.length, IPAddress, senderPort);
					serverSocket.send(sendPacket);
					
					// Commencer le televersement du fichier
					int bytesLeft = fileSize;
					int start = 0;
					int stop = PACKET_SIZE;
					// envoyer le fichier en morceaux
					while (bytesLeft > 0) {
						// vers le client
						out = Arrays.copyOfRange(byteFile, start, stop);
						sendPacket = new DatagramPacket(out, out.length, IPAddress, senderPort);
						serverSocket.send(sendPacket);
						
						// du client
						receivePacket = new DatagramPacket(in, in.length);
					    serverSocket.receive(receivePacket);
					    // flush
					    in = new byte[PACKET_SIZE];
					    start = start + PACKET_SIZE;
					    stop = stop + PACKET_SIZE;
					    bytesLeft = bytesLeft - PACKET_SIZE;
					}
				}
				break;
				
			case "back":
				this.serverSocket.close();
				flag = true;
				break;
			}
		}
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				this.setUpUDP();
				this.clientLoop();
			} catch (IOException | ClassNotFoundException se) {
				System.out.println("Connection lost. Unbinding");
				this.serverSocket.close();
			}
		}
	}
	
}