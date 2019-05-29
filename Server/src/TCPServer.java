
import java.io.BufferedInputStream;
import java.util.ArrayList;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TCPServer implements Runnable {
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd@HH:mm:ss");
	//private static final String SERVER_URL = "./tcp/images/";
	private static final String SERVER_URL = System.getProperty("user.dir") + File.separator + "tcp"
			+ File.separator + "images" + File.separator;
	private int port;
	ServerSocket serverSocket = null;
	Socket socket = null;
	ObjectInputStream in = null;
	ObjectOutputStream out = null;

	public TCPServer(int port) {
		this.port = port;
		this.printIPV4();
	}

	/**
	 * @param socket le socket du client
	 * @param command la commande du client
	 */
	private void logger(Socket socket, String command) {
		String remoteIP = socket.getRemoteSocketAddress().toString();
		System.out.println("[" + remoteIP.substring(1, remoteIP.length()) + " - "
				+ TCPServer.dtf.format(LocalDateTime.now()) + "]: " + command);
	}

	/**
	 * imprime les infos du serveur à l'écran
	 */
	private void printIPV4() {
		try {
			InetAddress inetAddress;
			inetAddress = InetAddress.getLocalHost();
			System.out.println("TCP Server @"+" IP Address:- " 
					+ inetAddress.getHostAddress() + " Port:- " + this.port);
		} catch (UnknownHostException e) {
			System.out.println("Could not obtain IP. Exiting...");
			System.exit(1);
		}

	}

	private void setUpTCP() throws IOException {
		this.serverSocket = new ServerSocket(this.port);
		this.socket = serverSocket.accept();
		System.out.println("Connection accepted.");
	}

	/**
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * gère les commandes d'un client jusqu'à un "back"
	 */
	private void clientLoop() throws IOException, ClassNotFoundException {
		boolean flag = false;

		while (!flag) {
			// recevoir la commande
			in = new ObjectInputStream(new BufferedInputStream(this.socket.getInputStream()));
			String clientCommand = (String) in.readObject();
			String[] terms = clientCommand.split(" ");
			String command = terms[0];

			this.logger(this.socket, clientCommand);
			switch (command) {
			case "ls":
				File folder = new File(SERVER_URL);
				File[] listOfFiles = folder.listFiles();
				ArrayList<String> fileNames = new ArrayList<String>();
				//créer un array de string contenant les nom des fichiers et des dossiers
				for (File file : listOfFiles) {
					if (file.isFile()) {
						fileNames.add("[File]\t\t" + file.getName());
					} else if (file.isDirectory()) {
						fileNames.add("[Folder]\t" + file.getName());
					}
				}
				//envoyer l'array de noms
				out = new ObjectOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
				out.writeObject(fileNames);
				out.flush();
				break;

			case "download":
				String filename = clientCommand.substring(9,clientCommand.length());
				boolean fileExists = true;
				File file =  new File(SERVER_URL+filename);
				InputStream in =  null;
				try{
		        in = new FileInputStream(file);
				}catch(FileNotFoundException e){
					fileExists = false;
				}
				
				long fileLength = (fileExists) ? file.length() : 0;
				
				//Envoyer la taille du fichier demandé. Si 0, le fichier n'existe pas
				ObjectOutputStream objectOutput = new ObjectOutputStream(this.socket.getOutputStream());
				objectOutput.writeObject(fileLength);
				objectOutput.flush();
				if(!fileExists){
					break;
				}
				
				// envoyer le fichier en morceaux
		        OutputStream out = this.socket.getOutputStream();
		        byte[] bytes = new byte[8192];

		        int count;
		        while ((count = in.read(bytes)) > 0) {
		            out.write(bytes, 0, count);
		        }
		        out.flush();
		        break;

			case "back":
				this.serverSocket.close();
				this.socket.close();
				flag = true;
				break;
			}
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				this.setUpTCP();
				this.clientLoop();
			} catch (IOException | ClassNotFoundException se) {
				System.out.println("Connection lost. Unbinding");
				try {
					this.socket.close();
					this.serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
