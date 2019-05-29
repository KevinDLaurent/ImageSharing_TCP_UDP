import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {

	private static final int MIN_PORT = 5001;
	private static final int MAX_PORT = 5050;
	public static final String COMMAND_DL = "download";
	public static final String COMMAND_LS = "ls";
	public static final String COMMAND_BACK = "back";
	private static final int PACKET_SIZE = 8192;

	private String ip;
	private int port;
	private boolean isInTCPMode;
	private Scanner scanner;
	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;

	public Client() throws IOException {
		this.scanner = new Scanner(System.in);
		this.ip = this.getIP();
	}

	/**
	 * boucle qui vérifie que l'entrée de l'usager est une adresse IPV4 valide
	 * 
	 * @return une adresse IPV4 en string
	 */
	private String getIP() {
		boolean isIPValid = false;
		String input = null;

		while (!isIPValid) {
			System.out.println("Please enter the server's IP address (ex.: 127.0.0.1).");
			input = this.scanner.nextLine().trim();
			String[] addressBytes = input.split("\\.");

			if (addressBytes.length != 4) {
				System.out.println("The input address has to be 4 bytes long.");
			} else {
				try {
					for (String addressByte : addressBytes) {
						int temp = Integer.parseInt(addressByte);
						if (temp < 0 || temp > 255) {
							throw new IllegalArgumentException();
						}
					}
					isIPValid = true;
				} catch (NumberFormatException nfe) {
					System.out.println("IP address must be made of integers.");
				} catch (IllegalArgumentException iae) {
					System.out.println("IP address must consist of bytes (between 0 and 255).");
				}
			}

		}
		return input;
	}

	/**
	 * @param protocol
	 *            TCP ou UDP. Désigne quel port l'usager choisit.
	 * @return un port valide
	 */
	private int getPort(String protocol) {

		int port = -1;
		String input;

		while (port < MIN_PORT || port > MAX_PORT) {
			System.out.println(
					"Enter a port between " + MIN_PORT + " and " + MAX_PORT + " for the " + protocol + " server.");
			input = this.scanner.nextLine();
			input = input.trim();

			try {
				port = Integer.parseInt(input);
			} catch (NumberFormatException nfe) {
				System.out.println("Error! Input is not an integer.");
			}
		}
		return port;
	}

	public void serverConsole() throws IOException, ClassNotFoundException {

		Socket socketTCP = null;
		DatagramSocket socketUDP = null;
		String input = "";

		while (true) {
			while (true) {
				System.out.println("Select a server: TCP or UDP. You may also enter \"exit\"");
				input = this.scanner.nextLine().toLowerCase().trim();
				if (input.equals("exit")) {
					System.out.println("Closing the console.");
					return;
				} else if (input.equals("tcp")) {
					this.isInTCPMode = true;
					break;
				} else if (input.equals("udp")) {
					this.isInTCPMode = false;
					break;
				}
			}

			this.port = getPort((this.isInTCPMode) ? "TCP" : "UDP");

			if (this.isInTCPMode == true) {
				socketTCP = new Socket(this.ip, this.port);
			} else {
				socketUDP = new DatagramSocket();
			}

			while (true) {
				System.out.println("Please select an action:\n" + "ls:\tList available files\n"
						+ "download <filename>:\tDownload file in the selected folder\n"
						+ "back:\tReturn to server and port selection");
				input = this.scanner.nextLine().trim();

				if (input.equals(Client.COMMAND_LS)) {
					if (this.isInTCPMode) {
						this.lsTCP(socketTCP);
					} else {
						this.lsUDP(socketUDP);
					}
				}else if (input.split(" ")[0].toLowerCase().equals(Client.COMMAND_DL)) {
					String filename = input.substring(Client.COMMAND_DL.length(), input.length()).trim();
					if (this.isInTCPMode) {
						this.downloadTCP(socketTCP, filename);
					} else {
						// UDP Image
						this.downloadUDP(socketUDP, filename);
					}
				} else if (input.equals(Client.COMMAND_BACK)){
					break;
				}else{
					System.out.println("Unrecognized command!");
				}

			}

			if (this.isInTCPMode) {
				this.sendStringTCP(socketTCP, Client.COMMAND_BACK);
				socketTCP.close();
			} else {
				socketUDP.close();
			}
		}

	}

	/**
	 * @param socket
	 * @throws IOException
	 * @throws ClassNotFoundException
	 *             envoie la commande ls et afficher la liste de noms renvoyée
	 */
	private void lsTCP(Socket socket) throws IOException, ClassNotFoundException {
		this.sendStringTCP(socket, "ls");
		ObjectInputStream obj = new ObjectInputStream(socket.getInputStream());
		ArrayList<String> filenames = (ArrayList<String>) obj.readObject();
		for (String name : filenames) {
			System.out.println(name);
		}
		System.out.println("");
	}

	/**
	 * @param socket
	 * @throws IOException
	 * @throws ClassNotFoundException
	 *             recoit la string des noms, la divise et l'affiche
	 */
	private void lsUDP(DatagramSocket socket) throws IOException, ClassNotFoundException {
		this.sendStringUDP(socket, "ls");
		byte[] receiveData = new byte[10024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		socket.receive(receivePacket);
		String out = fetchCommand(receiveData);
		String[] files = out.split("!!DELIM!!");
		for (String f : files) {
			System.out.println(f);
		}
		System.out.println("");
	}

	/**
	 * @param command
	 *            en byte[]
	 * @return commande en string
	 */
	public static String fetchCommand(byte[] command) {
		if (command == null)
			return null;
		StringBuilder build = new StringBuilder();
		int i = 0;
		while (command[i] != 0) {
			build.append((char) command[i]);
			i++;
		}
		return build.toString();
	}

	private void downloadTCP(Socket socket, String filename) throws IOException, ClassNotFoundException {
		// String fileURL = "./" + ((this.isInTCPMode) ? "tcp" : "udp") +
		// "/images/" + filename;
		String fileURL = System.getProperty("user.dir") + File.separator + "tcp"
				+ File.separator + "images" + File.separator + filename;
		File file = new File(fileURL);
		if (file.exists()) {
			System.out.println("The file already exists in the local directory.\n");
			return;
		}

		// envoie le nom du fichier
		this.sendStringTCP(socket, "download " + filename);
		// lire la longueur du fichier
		ObjectInputStream obj = new ObjectInputStream(socket.getInputStream());
		long fileSize = (long) obj.readObject();
		if (fileSize == 0) {
			System.out.println("The requested file does not exist on the server.\n");
			return;
		}
		// telecharger
		InputStream in = socket.getInputStream();
		OutputStream out = new FileOutputStream(fileURL);
		System.out.println("Downloading...");
		byte[] bytes = new byte[8192];
		int count = 0;
		int subSize;
		System.out.println("File size: " + fileSize + " bytes");

		while (count < fileSize) {
			count += (subSize = in.read(bytes));
			out.write(bytes, 0, subSize);
		}
		System.out.println("File Downloaded\n");
		out.close();

	}

	private void downloadUDP(DatagramSocket socket, String filename) throws IOException {
		//String fileURL = "./" + ((this.isInTCPMode) ? "tcp" : "udp") + "/images/" + filename;
		String fileURL = System.getProperty("user.dir") + File.separator + "udp"
				+ File.separator + "images" + File.separator + filename;
		File file = new File(fileURL);
		if (file.exists()) {
			System.out.println("The file already exists in the local directory.\n");
			return;
		}

		// attend le premier message du serveur: la taille du fichier
		this.sendStringUDP(socket, "download " + filename);
		byte[] receiveData = new byte[PACKET_SIZE];
		byte[] sendData = new byte[PACKET_SIZE];
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		socket.receive(receivePacket);

		String msg = fetchCommand(receiveData);
		int fileSize = Integer.valueOf(msg);

		if (fileSize == 0) {
			System.out.println("The requested file does not exist on the server.\n");
			return;
		}

		// telecharger
		System.out.println("Downloading...");
		System.out.println("File size: " + fileSize + " bytes");

		OutputStream out = new FileOutputStream(fileURL);

		int bytesLeft = fileSize;
		int start = 0;
		int stop = PACKET_SIZE;
		// on telecharge le fichier en morceaux
		while (bytesLeft > 0) {
			// du serveur
			if (bytesLeft < PACKET_SIZE) {
				// inutile de lire des données que l'on sait n'existe pas.
				receiveData = new byte[bytesLeft];
				receivePacket = new DatagramPacket(receiveData, receiveData.length);
			}
			socket.receive(receivePacket);
			out.write(receiveData);

			// vers le serveur
			String ack = "ACK";
			sendData = ack.getBytes();
			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(ip), port);
			socket.send(sendPacket);
			start = start + PACKET_SIZE;
			stop = stop + PACKET_SIZE;
			bytesLeft = bytesLeft - PACKET_SIZE;
		}

		out.close();
		System.out.println("File Downloaded\n");
	}

	private void sendStringTCP(Socket socket, String msgToServer) throws IOException {
		ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
		objectOutput.writeObject(msgToServer);
		objectOutput.flush();
	}

	private void sendStringUDP(DatagramSocket socket, String msgToServer) throws IOException {
		byte[] sendData = null;
		sendData = msgToServer.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(this.ip),
				this.port);
		socket.send(sendPacket);
	}

}
